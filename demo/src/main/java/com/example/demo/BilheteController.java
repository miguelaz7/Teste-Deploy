package com.example.demo;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/bilhetes")
@CrossOrigin(origins = "*")
public class BilheteController {

    private final BilheteRepository bilheteRepository;
    private final BilheteRealtimeService realtimeService;

    public BilheteController(BilheteRepository bilheteRepository, BilheteRealtimeService realtimeService) {
        this.bilheteRepository = bilheteRepository;
        this.realtimeService = realtimeService;
    }

    @GetMapping
    public List<Bilhete> listBilhetes(
        @RequestParam(required = false) String codigo,
        @RequestParam(required = false) String nome,
        @RequestParam(required = false) BigDecimal minPreco,
        @RequestParam(required = false) BigDecimal maxPreco,
        @RequestParam(defaultValue = "false") boolean soDisponiveis
    ) {
        Specification<Bilhete> specification = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

        if (hasText(codigo)) {
            String codigoFiltro = codigo.trim().toLowerCase();
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("codigo")), "%" + codigoFiltro + "%")
            );
        }

        if (hasText(nome)) {
            String nomeFiltro = nome.trim().toLowerCase();
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.like(criteriaBuilder.lower(root.get("nome")), "%" + nomeFiltro + "%")
            );
        }

        if (minPreco != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get("preco"), minPreco)
            );
        }

        if (maxPreco != null) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get("preco"), maxPreco)
            );
        }

        if (soDisponiveis) {
            specification = specification.and((root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThan(root.get("quantidade"), 0)
            );
        }

        return bilheteRepository.findAll(specification, Sort.by(Sort.Direction.ASC, "nome"));
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBilhetes() {
        return realtimeService.registerEmitter();
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
