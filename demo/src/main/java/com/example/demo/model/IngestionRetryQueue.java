package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public IngestionRetryQueue() {
    }

    public IngestionRetryQueue(String batchId, String reason, String status, OffsetDateTime createdAt) {
        this.batchId = batchId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }
}
