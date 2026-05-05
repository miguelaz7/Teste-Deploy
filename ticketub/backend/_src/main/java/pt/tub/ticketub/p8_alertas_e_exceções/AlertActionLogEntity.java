package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alert_action_log")
public class AlertActionLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_incident_id", nullable = false)
    private AlertIncidentEntity alertIncident;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 40)
    private AlertActionType actionType;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "resulting_status", nullable = false, length = 30)
    private AlertStatus resultingStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public AlertIncidentEntity getAlertIncident() {
        return alertIncident;
    }

    public void setAlertIncident(AlertIncidentEntity alertIncident) {
        this.alertIncident = alertIncident;
    }

    public AlertActionType getActionType() {
        return actionType;
    }

    public void setActionType(AlertActionType actionType) {
        this.actionType = actionType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public AlertStatus getResultingStatus() {
        return resultingStatus;
    }

    public void setResultingStatus(AlertStatus resultingStatus) {
        this.resultingStatus = resultingStatus;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}