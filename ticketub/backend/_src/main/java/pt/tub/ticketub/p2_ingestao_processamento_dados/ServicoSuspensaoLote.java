package pt.tub.ticketub.p2_ingestao_processamento_dados;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServicoSuspensaoLote {

    private final RepositorioControloLoteIngestao batchControlRepository;
    private final RepositorioEstatisticasLoteIngestao batchStatsRepository;
    private final RepositorioAuditoriaIngestao auditLogRepository;

    public ServicoSuspensaoLote(
        RepositorioControloLoteIngestao batchControlRepository,
        RepositorioEstatisticasLoteIngestao batchStatsRepository,
        RepositorioAuditoriaIngestao auditLogRepository
    ) {
        this.batchControlRepository = batchControlRepository;
        this.batchStatsRepository = batchStatsRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void suspenderLote(String batchId) {
        if (batchId == null) return;

        batchControlRepository.findById(batchId).ifPresent(batch -> {
            batch.setStatus("SUSPENSO");
            batchControlRepository.save(batch);
        });

        batchStatsRepository.findByBatchId(batchId).ifPresent(stats -> {
            stats.setEstado("SUSPENSO");
            batchStatsRepository.save(stats);
        });

        auditLogRepository.save(new RegistoAuditoriaIngestao(
            "BATCH_SUSPENDED", "batchId", batchId,
            "Lote suspenso devido a detecao de PII na classificacao",
            java.time.OffsetDateTime.now()
        ));
    }
}
