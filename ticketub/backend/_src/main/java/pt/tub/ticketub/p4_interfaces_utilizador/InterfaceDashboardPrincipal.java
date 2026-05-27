package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEstatisticasLoteIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

// =============================================================================
// O0.4.1.i – Interface da Dashboard Principal (UC04.1)
// Apresenta os widgets principais: métricas de procura, estado de ingestão,
// alertas activos e atalhos para módulos. Conteúdo adaptado ao perfil RBAC.
// Consome dados de: O0.2.4.d (estatísticas ingestão), O0.5.1.d (procura),
// O0.9.1.d (alertas).
// =============================================================================

@RestController
@RequestMapping("/api/dashboard")
public class InterfaceDashboardPrincipal {

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioQuarentenaValidacao validationQuarantineRepository;
    private final RepositorioEstatisticasLoteIngestao statsRepository;

    public InterfaceDashboardPrincipal(
        RepositorioEventoValidacao validationEventRepository,
        RepositorioQuarentenaValidacao validationQuarantineRepository,
        RepositorioEstatisticasLoteIngestao statsRepository
    ) {
        this.validationEventRepository    = validationEventRepository;
        this.validationQuarantineRepository = validationQuarantineRepository;
        this.statsRepository  = statsRepository;
    }

    // Widget: métricas de estado de ingestão por janela temporal (O0.2.4.d)
    @GetMapping("/metricas-ingestao")
    public ResponseEntity<Map<String, Object>> getIngestionMetrics() {
        OffsetDateTime agora = OffsetDateTime.now();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("ultimaHora",     calculatePeriod(agora.minus(Duration.ofHours(1))));
        resposta.put("ultimas24Horas", calculatePeriod(agora.minus(Duration.ofHours(24))));
        resposta.put("ultimos7Dias",   calculatePeriod(agora.minus(Duration.ofDays(7))));

        return ResponseEntity.ok(resposta);
    }

    private Map<String, Object> calculatePeriod(OffsetDateTime inicio) {
        // Válidas: lidas directamente de validation_events
        long validas = validationEventRepository.countByIngestedAtAfter(inicio);

        // Quarentena: lidas de validation_quarantine
        long quarentena = validationQuarantineRepository.countByCreatedAtAfter(inicio);

        // Duplicados: somados a partir de ingestion_batch_stats (O0.2.4.d)
        long duplicados = statsRepository.findAll().stream()
            .filter(s -> s.getTimestampCiclo() != null
                && s.getTimestampCiclo().isAfter(inicio))
            .mapToLong(s -> s.getTotalDuplicados())
            .sum();

        long total          = validas + quarentena + duplicados;
        long totalQualidade = validas + quarentena;

        // Nomes de campos alinhados com o que o PainelGeral.js espera
        Map<String, Object> periodo = new LinkedHashMap<>();
        periodo.put("volumeIngestao", total);
        periodo.put("validas",        validas);
        periodo.put("quarentena",     quarentena);
        periodo.put("duplicados",     duplicados);
        periodo.put("taxaValidas",    percentage(validas, totalQualidade));
        periodo.put("taxaQuarentena", percentage(quarentena, totalQualidade));
        periodo.put("estado",         resolveStatus(quarentena, totalQualidade));
        return periodo;
    }

    private String resolveStatus(long quarentena, long total) {
        if (total == 0) return "SEM_DADOS";
        double taxa = (double) quarentena / total;
        if (taxa > 0.10) return "CRITICO";
        if (taxa > 0.05) return "AVISO";
        return "NORMAL";
    }

    private double percentage(long parte, long total) {
        if (total <= 0) return 0.0;
        return BigDecimal.valueOf(parte)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }
}