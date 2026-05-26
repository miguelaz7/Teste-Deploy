package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// =============================================================================
// O011.2.i – Interface de Integração ERP (UC11.2)
// Permite à equipa financeira consultar dados exportados para o ERP,
// comparar receita com objectivos e gerar relatórios de controlo.
// Consome dados de: O011.2.d (dados estruturados para ERP).
// =============================================================================

@RestController
@RequestMapping("/api/erp")
public class InterfaceIntegracaoERP {

    private final ValidationEventRepository validationEventRepository;

    public InterfaceIntegracaoERP(ValidationEventRepository validationEventRepository) {
        this.validationEventRepository = validationEventRepository;
    }

    // Dados financeiros por linha e título para consumo pelo ERP
    @GetMapping("/dados-financeiros")
    public ResponseEntity<Map<String, Object>> obterDadosFinanceiros(
        @RequestParam(defaultValue = "30") int dias
    ) {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(dias);

        List<Object[]> porLinha = validationEventRepository.countByRouteIdAfter(desde);
        List<Object[]> porTipo  = validationEventRepository.countByTicketTypeAfter(desde);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("perioDias",     dias);
        resposta.put("geradoEm",      OffsetDateTime.now());
        resposta.put("porLinha",      toListMap(porLinha, "routeId", "total"));
        resposta.put("porTipoTitulo", toListMap(porTipo, "tipoTitulo", "total"));

        return ResponseEntity.ok(resposta);
    }

    private List<Map<String, Object>> toListMap(List<Object[]> rows, String chave1, String chave2) {
        return rows.stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put(chave1, row[0] != null ? row[0].toString() : "desconhecido");
            m.put(chave2, ((Number) row[1]).longValue());
            return m;
        }).collect(Collectors.toList());
    }

    // UC11.2 — Gerar e registar exportação para ERP
    @PostMapping("/dados-financeiros")
    public ResponseEntity<Map<String, Object>> gerarParaERP(
        @RequestBody(required = false) Map<String, Object> params
    ) {
        int dias = params != null && params.containsKey("dias")
            ? Integer.parseInt(params.get("dias").toString()) : 30;
        return obterDadosFinanceiros(dias);
    }
}