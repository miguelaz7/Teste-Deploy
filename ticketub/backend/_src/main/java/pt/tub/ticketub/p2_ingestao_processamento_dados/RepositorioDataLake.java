package pt.tub.ticketub.p2_ingestao_processamento_dados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// =============================================================================
// O0.2.4.d – Repositório do Data Lake
// Armazena entidades normalizadas e anonimizadas, com metadados de lote
// e estado de integridade da escrita. Inclui fila de retry para falhas
// de persistência.
// =============================================================================

// --- Controlo de lote: metadados e estado de integridade por lote ---
@Entity
@Table(name = "ingestion_batch_control")
class ControloLoteIngestao {

    @Id
    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "entity_count_expected", nullable = false)
    private Integer entityCountExpected;

    @Column(name = "entity_count_persisted", nullable = false)
    private Integer entityCountPersisted;

    @Column(name = "pipeline_version", nullable = false)
    private String pipelineVersion;

    @Column(name = "partition_date", nullable = false)
    private LocalDate partitionDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "persistence_time_ms", nullable = false)
    private Long persistenceTimeMs;

    ControloLoteIngestao() {}

    ControloLoteIngestao(String batchId, OffsetDateTime createdAt,
                          Integer entityCountExpected, Integer entityCountPersisted,
                          String pipelineVersion, LocalDate partitionDate,
                          String status, Long persistenceTimeMs) {
        this.batchId               = batchId;
        this.createdAt             = createdAt;
        this.entityCountExpected   = entityCountExpected;
        this.entityCountPersisted  = entityCountPersisted;
        this.pipelineVersion       = pipelineVersion;
        this.partitionDate         = partitionDate;
        this.status                = status;
        this.persistenceTimeMs     = persistenceTimeMs;
    }

    public String getBatchId() { return batchId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public Integer getEntityCountExpected() { return entityCountExpected; }
    public Integer getEntityCountPersisted() { return entityCountPersisted; }
    public String getPipelineVersion() { return pipelineVersion; }
    public LocalDate getPartitionDate() { return partitionDate; }
    public String getStatus() { return status; }
    public Long getPersistenceTimeMs() { return persistenceTimeMs; }
}

@Repository
interface RepositorioControloLoteIngestao extends JpaRepository<ControloLoteIngestao, String> {
}

// --- Fila de retry: lotes que falharam a persistência no Data Lake ---
@Entity
@Table(name = "ingestion_retry_queue")
class FilaRetryIngestao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "reason", nullable = false, length = 2000)
    private String reason;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "next_retry_at", nullable = false)
    private OffsetDateTime nextRetryAt;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    FilaRetryIngestao() {}

    FilaRetryIngestao(String batchId, String reason, String status,
                        Integer retryCount, Integer maxRetries,
                        OffsetDateTime nextRetryAt, OffsetDateTime lastAttemptAt,
                        String payloadJson, OffsetDateTime createdAt) {
        this.batchId       = batchId;
        this.reason        = reason;
        this.status        = status;
        this.retryCount    = retryCount;
        this.maxRetries    = maxRetries;
        this.nextRetryAt   = nextRetryAt;
        this.lastAttemptAt = lastAttemptAt;
        this.payloadJson   = payloadJson;
        this.createdAt     = createdAt;
    }

    Long getId()                               { return id; }
    String getBatchId()                        { return batchId; }
    String getReason()                         { return reason; }
    void setReason(String reason)              { this.reason = reason; }
    String getStatus()                         { return status; }
    void setStatus(String status)              { this.status = status; }
    Integer getRetryCount()                    { return retryCount; }
    void setRetryCount(Integer retryCount)     { this.retryCount = retryCount; }
    Integer getMaxRetries()                    { return maxRetries; }
    OffsetDateTime getNextRetryAt()            { return nextRetryAt; }
    void setNextRetryAt(OffsetDateTime t)      { this.nextRetryAt = t; }
    OffsetDateTime getLastAttemptAt()          { return lastAttemptAt; }
    void setLastAttemptAt(OffsetDateTime t)    { this.lastAttemptAt = t; }
    String getPayloadJson()                    { return payloadJson; }
    OffsetDateTime getCreatedAt()              { return createdAt; }
}

@Repository
interface RepositorioFilaRetryIngestao extends JpaRepository<FilaRetryIngestao, Long> {
    List<FilaRetryIngestao> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(
        String status, OffsetDateTime reference);
}
