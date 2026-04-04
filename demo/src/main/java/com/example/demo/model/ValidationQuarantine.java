package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "validation_quarantine")
public class ValidationQuarantine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_line", nullable = false, length = 2000)
    private String rawLine;

    @Column(name = "reason", nullable = false)
    private String reason;

    @Column(name = "invalid_field")
    private String invalidField;

    @Column(name = "received_value", length = 1000)
    private String receivedValue;

    @Column(name = "violated_rule", length = 1000)
    private String violatedRule;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public ValidationQuarantine() {
    }

    public ValidationQuarantine(String rawLine, String reason, OffsetDateTime createdAt) {
        this.rawLine = rawLine;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public ValidationQuarantine(
        String rawLine,
        String reason,
        String invalidField,
        String receivedValue,
        String violatedRule,
        OffsetDateTime createdAt
    ) {
        this.rawLine = rawLine;
        this.reason = reason;
        this.invalidField = invalidField;
        this.receivedValue = receivedValue;
        this.violatedRule = violatedRule;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getRawLine() {
        return rawLine;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
