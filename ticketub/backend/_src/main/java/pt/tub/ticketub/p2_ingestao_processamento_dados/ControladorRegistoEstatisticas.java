package pt.tub.ticketub.p2_ingestao_processamento_dados;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

// =============================================================================
// O0.2.4.c – Controlador de Registo de Estatísticas e Auditoria
// Consolida métricas de ingestão, grava o resumo de auditoria do lote
// em O0.2.4.d e disponibiliza o estado final do processamento.
// Gera alerta de qualidade de dados se taxa de falhas > 5%.
// =============================================================================

@Service
class ControladorRegistoEstatisticas {

    private static final double LIMIAR_QUALIDADE = 0.05;

    private final IngestionBatchStatsRepository batchStatsRepository;
    private final IngestionAuditLogRepository auditLogRepository;

    ControladorRegistoEstatisticas(IngestionBatchStatsRepository batchStatsRepository,
                                   IngestionAuditLogRepository auditLogRepository) {
        this.batchStatsRepository = batchStatsRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // Grava as estatísticas do ciclo em O0.2.4.d e gera alerta se necessário
    void registar(String batchId, int total, int validos, int invalidos,
                  int duplicados, List<String> motivosRejeicao, OffsetDateTime inicioCiclo) {

        long tempoMs = Duration.between(inicioCiclo, OffsetDateTime.now()).toMillis();
        boolean alertaQualidade = total > 0 && ((double) invalidos / total) > LIMIAR_QUALIDADE;

        // Resumo agrupado dos motivos de rejeição
        String resumoMotivos = motivosRejeicao.stream()
            .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
            .entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(", "));

        String estado = invalidos == 0 ? "SUCESSO" : (validos > 0 ? "PARCIAL" : "FALHA");

        // O0.2.4.d: gravar estatísticas estruturadas por lote
        batchStatsRepository.save(new IngestionBatchStats(
            batchId,
            OffsetDateTime.now(),
            total, validos, invalidos, duplicados,
            tempoMs, estado,
            resumoMotivos.isEmpty() ? null : resumoMotivos,
            alertaQualidade
        ));

        // Log de auditoria com resumo do ciclo
        auditar("INGESTION_CYCLE_STATS", "batchId", batchId,
            String.format("total=%d validos=%d invalidos=%d duplicados=%d tempoMs=%d alerta=%b",
                total, validos, invalidos, duplicados, tempoMs, alertaQualidade));

        // Alerta de qualidade de dados (UC02.4)
        if (alertaQualidade) {
            auditar("DATA_QUALITY_ALERT", "taxaFalha",
                String.format("%.2f%%", (double) invalidos / total * 100),
                String.format("ALERTA: taxa de falha %.1f%% excede limiar de %.0f%%. batchId=%s",
                    (double) invalidos / total * 100, LIMIAR_QUALIDADE * 100, batchId));
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void auditar(String tipo, String campo, String valor, String detalhe) {
        auditLogRepository.save(
            new IngestionAuditLog(tipo, campo, valor, detalhe, OffsetDateTime.now()));
    }
}
