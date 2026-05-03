package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantine;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantineRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// =============================================================================
// O09.2.i – Interface de Gestão de Alertas (UC09.2)
// Painel para a equipa de fiscalização consultar alertas por severidade,
// registar acção tomada e gerar mapa de calor.
// Consome dados de: O0.9.2.c e O0.9.1.d (repositório de alertas e anomalias).
// =============================================================================

@RestController
@RequestMapping("/api/alertas")
public class InterfaceGestaoAlertas {

    private final ValidationQuarantineRepository validationQuarantineRepository;

    public InterfaceGestaoAlertas(ValidationQuarantineRepository validationQuarantineRepository) {
        this.validationQuarantineRepository = validationQuarantineRepository;
    }

    // Resumo de alertas ativos agrupados por motivo e severidade
    @GetMapping("/ativos")
    public ResponseEntity<Map<String, Object>> obterAlertasAtivos(
        @RequestParam(defaultValue = "24") int horas
    ) {
        OffsetDateTime desde = OffsetDateTime.now().minusHours(horas);
        List<ValidationQuarantine> quarentena = validationQuarantineRepository.findAll().stream()
            .filter(q -> q.getCreatedAt().isAfter(desde))
            .collect(Collectors.toList());

        // Agrupar por motivo de rejeição
        Map<String, Long> porMotivo = quarentena.stream()
            .collect(Collectors.groupingBy(
                q -> q.getReason() != null ? q.getReason() : "desconhecido",
                Collectors.counting()
            ));

        // Classificar severidade: CRITICO > 10%, AVISO > 5%, NORMAL
        long total = quarentena.size();
        String severidade;
        if (total > 0) {
            long maxMotivo = porMotivo.values().stream().mapToLong(Long::longValue).max().orElse(0);
            double taxaMax = (double) maxMotivo / Math.max(total, 1);
            severidade = taxaMax > 0.10 ? "CRITICO" : (taxaMax > 0.05 ? "AVISO" : "NORMAL");
        } else {
            severidade = "NORMAL";
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("totalAlertas",   total);
        resposta.put("severidade",     severidade);
        resposta.put("janelaHoras",    horas);
        resposta.put("porMotivo",      porMotivo);

        return ResponseEntity.ok(resposta);
    }

    // Lista detalhada de registos em quarentena para revisão
    @GetMapping("/quarentena")
    public ResponseEntity<List<Map<String, Object>>> obterQuarentena(
        @RequestParam(defaultValue = "24") int horas
    ) {
        OffsetDateTime desde = OffsetDateTime.now().minusHours(horas);
        List<Map<String, Object>> lista = validationQuarantineRepository.findAll().stream()
            .filter(q -> q.getCreatedAt().isAfter(desde))
            .map(q -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",         q.getId());
                item.put("motivo",     q.getReason());
                item.put("criadoEm",   q.getCreatedAt());
                return item;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }
}
