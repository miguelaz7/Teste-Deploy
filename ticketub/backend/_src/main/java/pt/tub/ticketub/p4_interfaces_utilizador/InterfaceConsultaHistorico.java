package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// =============================================================================
// O010.2.i – Interface de Consulta de Histórico (UC10.2)
// Interface para a direcção consultar séries temporais, comparar períodos
// e exportar relatórios com metadados de rastreabilidade.
// Consome dados de: O010.1.d (histórico consolidado).
// =============================================================================

@RestController
@RequestMapping("/api/historico")
public class InterfaceConsultaHistorico {

    private final ValidationEventRepository validationEventRepository;

    public InterfaceConsultaHistorico(ValidationEventRepository validationEventRepository) {
        this.validationEventRepository = validationEventRepository;
    }

    // Comparação entre dois períodos: validações totais e variação percentual
    @GetMapping("/comparacao-periodos")
    public ResponseEntity<Map<String, Object>> compararPeriodos(
        @RequestParam(defaultValue = "7")  int diasPeriodo1,
        @RequestParam(defaultValue = "14") int diasPeriodo2
    ) {
        OffsetDateTime agora = OffsetDateTime.now();

        long totalPeriodo1 = validationEventRepository
            .countByIngestedAtAfter(agora.minusDays(diasPeriodo1));
        long totalPeriodo2 = validationEventRepository
            .countByIngestedAtAfter(agora.minusDays(diasPeriodo2))
            - totalPeriodo1;

        double variacao = totalPeriodo2 > 0
            ? ((double)(totalPeriodo1 - totalPeriodo2) / totalPeriodo2) * 100.0
            : 0.0;

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("periodo1Dias",   diasPeriodo1);
        resposta.put("periodo1Total",  totalPeriodo1);
        resposta.put("periodo2Dias",   diasPeriodo2 - diasPeriodo1);
        resposta.put("periodo2Total",  totalPeriodo2);
        resposta.put("variacaoPercent", Math.round(variacao * 100.0) / 100.0);

        return ResponseEntity.ok(resposta);
    }
}
