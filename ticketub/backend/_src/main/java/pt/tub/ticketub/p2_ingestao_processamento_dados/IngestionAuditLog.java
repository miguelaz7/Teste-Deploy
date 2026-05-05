package pt.tub.ticketub.p2_ingestao_processamento_dados;

// O0.2.4.d – Repositório de Estatísticas de Ingestão (entidade de auditoria)

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_audit_log")
public class IngestionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "field_name")
    private String fieldName;

    @Column(name = "original_value", length = 1000)
    private String originalValue;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public IngestionAuditLog() {}

    public IngestionAuditLog(String eventType, String fieldName,
                             String originalValue, String detail, OffsetDateTime createdAt) {
        this.eventType     = eventType;
        this.fieldName     = fieldName;
        this.originalValue = originalValue;
        this.detail        = detail;
        this.createdAt     = createdAt;
    }

    public Long getId()              { return id; }
    public String getEventType()     { return eventType; }
    public String getFieldName()     { return fieldName; }
    public String getOriginalValue() { return originalValue; }
    public String getDetail()        { return detail; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
