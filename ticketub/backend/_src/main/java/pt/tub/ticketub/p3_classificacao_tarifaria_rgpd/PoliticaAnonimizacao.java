package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

// =============================================================================
// O0.3.2.d – Repositório de Políticas de Anonimização
// Regras RGPD aprovadas pelo DPO: campos a pseudonimizar, períodos de
// retenção e histórico de alterações. Consultado pelo UC02.2.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "politicas_anonimizacao")
public class PoliticaAnonimizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campo a pseudonimizar (ex: "cardId", "ticketId")
    @JsonProperty("campo")
    @Column(name = "campo", nullable = false)
    private String field;

    // Método de anonimização (ex: "HMAC_SHA256", "SUPRESSAO")
    @JsonProperty("metodo")
    @Column(name = "metodo", nullable = false)
    private String method;

    // Período de retenção em dias (-1 = indefinido)
    @JsonProperty("retencaoDias")
    @Column(name = "retencao_dias", nullable = false)
    private int retentionDays;

    // Estado da política: ATIVA, REVOGADA
    @JsonProperty("estado")
    @Column(name = "estado", nullable = false)
    private String status;

    // DPO que aprovou
    @JsonProperty("aprovadoPor")
    @Column(name = "aprovado_por", nullable = false)
    private String approvedBy;

    @JsonProperty("aprovadoEm")
    @Column(name = "aprovado_em", nullable = false)
    private OffsetDateTime approvedAt;

    @JsonProperty("notas")
    @Column(name = "notas", length = 1000)
    private String notes;

    public PoliticaAnonimizacao() {}

    public PoliticaAnonimizacao(String field, String method, int retentionDays,
                         String status, String approvedBy,
                         OffsetDateTime approvedAt, String notes) {
        this.field         = field;
        this.method        = method;
        this.retentionDays = retentionDays;
        this.status        = status;
        this.approvedBy    = approvedBy;
        this.approvedAt    = approvedAt;
        this.notes         = notes;
    }

    public Long getId()                         { return id; }
    public String getField()                    { return field; }
    public void setField(String field)          { this.field = field; }
    public String getMethod()                   { return method; }
    public void setMethod(String method)        { this.method = method; }
    public int getRetentionDays()               { return retentionDays; }
    public void setRetentionDays(int d)         { this.retentionDays = d; }
    public String getStatus()                   { return status; }
    public void setStatus(String status)        { this.status = status; }
    public String getApprovedBy()               { return approvedBy; }
    public void setApprovedBy(String a)         { this.approvedBy = a; }
    public OffsetDateTime getApprovedAt()       { return approvedAt; }
    public void setApprovedAt(OffsetDateTime a) { this.approvedAt = a; }
    public String getNotes()                    { return notes; }
    public void setNotes(String notes)          { this.notes = notes; }
}