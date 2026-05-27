package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// =============================================================================
// O0.9.1.d – Repositório de Alertas e Anomalias
// Repositório de alertas: tipo, severidade, timestamp, dados do evento
// e estado (PENDENTE / EM_ANALISE / RESOLVIDO / FALSO_POSITIVO).
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alertas_anomalias")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false)
    private String type;

    @Column(name = "severidade", nullable = false)
    private String severity;

    @Column(name = "ingestion_hash")
    private String ingestionHash;

    @Column(name = "dados_evento", length = 2000)
    private String eventData;

    @Column(name = "estado", nullable = false)
    private String status;

    @Column(name = "atribuido_a")
    private String assignedTo;

    @Column(name = "accao_resolucao", length = 1000)
    private String resolutionAction;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvedAt;

    public Alerta() {}

    public Alerta(String type, String severity, String ingestionHash,
                  String eventData, OffsetDateTime createdAt) {
        this.type          = type;
        this.severity      = severity;
        this.ingestionHash = ingestionHash;
        this.eventData     = eventData;
        this.status        = "PENDENTE";
        this.createdAt     = createdAt;
    }

    public Long getId()                          { return id; }
    public String getType()                      { return type; }
    public String getSeverity()                  { return severity; }
    public void setSeverity(String severity)     { this.severity = severity; }
    public String getIngestionHash()             { return ingestionHash; }
    public String getEventData()                 { return eventData; }
    public String getStatus()                    { return status; }
    public void setStatus(String status)         { this.status = status; }
    public String getAssignedTo()                { return assignedTo; }
    public void setAssignedTo(String a)          { this.assignedTo = a; }
    public String getResolutionAction()          { return resolutionAction; }
    public void setResolutionAction(String a)    { this.resolutionAction = a; }
    public OffsetDateTime getCreatedAt()          { return createdAt; }
    public OffsetDateTime getResolvedAt()       { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime t)  { this.resolvedAt = t; }
}
