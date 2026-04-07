package com.example.demo.service;

import com.example.demo.model.IngestionRetryQueue;
import com.example.demo.repository.IngestionRetryQueueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class IngestionRetryScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(IngestionRetryScheduler.class);

    private final IngestionRetryQueueRepository ingestionRetryQueueRepository;
    private final CsvNormalizationService csvNormalizationService;

    @Value("${app.ingestion.retry.enabled:true}")
    private boolean retryEnabled;

    public IngestionRetryScheduler(
        IngestionRetryQueueRepository ingestionRetryQueueRepository,
        CsvNormalizationService csvNormalizationService
    ) {
        this.ingestionRetryQueueRepository = ingestionRetryQueueRepository;
        this.csvNormalizationService = csvNormalizationService;
    }

    @Scheduled(fixedDelayString = "${app.ingestion.retry.poll-ms:5000}")
    public void processarRetriesPendentes() {
        if (!retryEnabled) {
            return;
        }

        OffsetDateTime agora = OffsetDateTime.now();
        List<IngestionRetryQueue> pendentes = ingestionRetryQueueRepository
            .findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc("PENDING", agora);

        for (IngestionRetryQueue item : pendentes) {
            tentarReprocessar(item);
        }
    }

    private void tentarReprocessar(IngestionRetryQueue item) {
        OffsetDateTime agora = OffsetDateTime.now();
        item.setStatus("PROCESSING");
        item.setLastAttemptAt(agora);
        ingestionRetryQueueRepository.save(item);

        boolean sucesso = csvNormalizationService.reprocessarLoteRetry(item);
        if (sucesso) {
            item.setStatus("SUCCESS");
            ingestionRetryQueueRepository.save(item);
            LOG.info("Retry concluido com sucesso para lote {}", item.getBatchId());
            return;
        }

        int tentativas = item.getRetryCount() + 1;
        item.setRetryCount(tentativas);
        if (tentativas >= item.getMaxRetries()) {
            item.setStatus("FAILED");
            ingestionRetryQueueRepository.save(item);
            LOG.error("Retry falhou definitivamente para lote {} apos {} tentativas", item.getBatchId(), tentativas);
            return;
        }

        item.setStatus("PENDING");
        item.setNextRetryAt(agora.plusSeconds(30));
        ingestionRetryQueueRepository.save(item);
        LOG.warn("Retry reagendado para lote {} (tentativa {}/{})", item.getBatchId(), tentativas, item.getMaxRetries());
    }
}
