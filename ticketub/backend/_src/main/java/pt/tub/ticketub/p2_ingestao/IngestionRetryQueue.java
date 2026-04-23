package pt.tub.ticketub.p2_ingestao;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_retry_queue")
public class IngestionRetryQueue {

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

    public IngestionRetryQueue() {
    }

    public IngestionRetryQueue(String batchId, String reason, String status, OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.reason = reason;
        this.status = status;
        this.retryCount = 0;
        this.maxRetries = 3;
        this.nextRetryAt = createdAt.plusSeconds(30);
        this.createdAt = createdAt;
    }

    public IngestionRetryQueue(
        String batchId,
        String reason,
        String status,
        Integer retryCount,
        Integer maxRetries,
        OffsetDateTime nextRetryAt,
        OffsetDateTime lastAttemptAt,
        String payloadJson,
        OffsetDateTime createdAt
    ) {
        this.batchId = batchId;
        this.reason = reason;
        this.status = status;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.nextRetryAt = nextRetryAt;
        this.lastAttemptAt = lastAttemptAt;
        this.payloadJson = payloadJson;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(OffsetDateTime nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public OffsetDateTime getLastAttemptAt() {
        return lastAttemptAt;
    }

    public void setLastAttemptAt(OffsetDateTime lastAttemptAt) {
        this.lastAttemptAt = lastAttemptAt;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}





