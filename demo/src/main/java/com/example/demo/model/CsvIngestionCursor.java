package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "csv_ingestion_cursor")
public class CsvIngestionCursor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "csv_path", nullable = false, unique = true, length = 1000)
    private String csvPath;

    @Column(name = "processed_data_lines", nullable = false)
    private long processedDataLines;

    @Column(name = "last_known_size", nullable = false)
    private long lastKnownSize;

    @Column(name = "last_known_modified_at", nullable = false)
    private long lastKnownModifiedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CsvIngestionCursor() {
    }

    public CsvIngestionCursor(String csvPath, long processedDataLines, long lastKnownSize, long lastKnownModifiedAt, OffsetDateTime updatedAt) {
        this.csvPath = csvPath;
        this.processedDataLines = processedDataLines;
        this.lastKnownSize = lastKnownSize;
        this.lastKnownModifiedAt = lastKnownModifiedAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getCsvPath() {
        return csvPath;
    }

    public void setCsvPath(String csvPath) {
        this.csvPath = csvPath;
    }

    public long getProcessedDataLines() {
        return processedDataLines;
    }

    public void setProcessedDataLines(long processedDataLines) {
        this.processedDataLines = processedDataLines;
    }

    public long getLastKnownSize() {
        return lastKnownSize;
    }

    public void setLastKnownSize(long lastKnownSize) {
        this.lastKnownSize = lastKnownSize;
    }

    public long getLastKnownModifiedAt() {
        return lastKnownModifiedAt;
    }

    public void setLastKnownModifiedAt(long lastKnownModifiedAt) {
        this.lastKnownModifiedAt = lastKnownModifiedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
