package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_batch_control")
public class IngestionBatchControl {

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

    public IngestionBatchControl() {
    }

    public IngestionBatchControl(
        String batchId,
        OffsetDateTime createdAt,
        Integer entityCountExpected,
        Integer entityCountPersisted,
        String pipelineVersion,
        LocalDate partitionDate,
        String status,
        Long persistenceTimeMs
    ) {
        this.batchId = batchId;
        this.createdAt = createdAt;
        this.entityCountExpected = entityCountExpected;
        this.entityCountPersisted = entityCountPersisted;
        this.pipelineVersion = pipelineVersion;
        this.partitionDate = partitionDate;
        this.status = status;
        this.persistenceTimeMs = persistenceTimeMs;
    }
}
