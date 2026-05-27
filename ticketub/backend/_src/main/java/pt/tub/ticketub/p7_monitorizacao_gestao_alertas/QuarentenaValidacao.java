package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "validation_quarantine")
public class QuarentenaValidacao {

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

    // UC06.3: necessário para consultar inválidas por paragem no popup LIVE DATA
    @Column(name = "origin_stop_id")
    private String originStopId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public QuarentenaValidacao() {}

    public QuarentenaValidacao(String rawLine, String reason, OffsetDateTime createdAt) {
        this.rawLine   = rawLine;
        this.reason    = reason;
        this.createdAt = createdAt;
    }

    public QuarentenaValidacao(String rawLine, String reason, String invalidField,
                                String receivedValue, String violatedRule,
                                OffsetDateTime createdAt) {
        this.rawLine      = rawLine;
        this.reason       = reason;
        this.invalidField = invalidField;
        this.receivedValue = receivedValue;
        this.violatedRule = violatedRule;
        this.createdAt    = createdAt;
    }

    // Construtor completo com originStopId
    public QuarentenaValidacao(String rawLine, String reason, String invalidField,
                                String receivedValue, String violatedRule,
                                String originStopId, OffsetDateTime createdAt) {
        this.rawLine       = rawLine;
        this.reason        = reason;
        this.invalidField  = invalidField;
        this.receivedValue = receivedValue;
        this.violatedRule  = violatedRule;
        this.originStopId  = originStopId;
        this.createdAt     = createdAt;
    }

    public Long getId()              { return id; }
    public String getRawLine()       { return rawLine; }
    public String getReason()        { return reason; }
    public String getInvalidField()  { return invalidField; }
    public String getReceivedValue() { return receivedValue; }
    public String getViolatedRule()  { return violatedRule; }
    public String getOriginStopId()  { return originStopId; }
    public void setOriginStopId(String originStopId) { this.originStopId = originStopId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
