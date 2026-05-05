package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "alert_incidents")
public class AlertIncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "alert_fingerprint", nullable = false, length = 300)
    private String alertFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 60)
    private AlertCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 30)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AlertStatus status;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Column(name = "route_id", length = 80)
    private String routeId;

    @Column(name = "stop_id", length = 80)
    private String stopId;

    @Column(name = "trip_id", length = 80)
    private String tripId;

    @Column(name = "card_id", length = 120)
    private String cardId;

    @Column(name = "ticket_id", length = 120)
    private String ticketId;

    @Column(name = "source_validation_event_id")
    private Long sourceValidationEventId;

    @Column(name = "observed_count", nullable = false)
    private int observedCount;

    @Column(name = "escalated_to_control_center", nullable = false)
    private boolean escalatedToControlCenter;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "last_occurrence_at")
    private OffsetDateTime lastOccurrenceAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "assigned_to", length = 120)
    private String assignedTo;

    @Column(name = "last_action_by", length = 120)
    private String lastActionBy;

    @Column(name = "last_action_note", length = 1000)
    private String lastActionNote;

    @Column(name = "false_positive_count", nullable = false)
    private int falsePositiveCount;

    public Long getId() {
        return id;
    }

    public String getAlertFingerprint() {
        return alertFingerprint;
    }

    public void setAlertFingerprint(String alertFingerprint) {
        this.alertFingerprint = alertFingerprint;
    }

    public AlertCategory getCategory() {
        return category;
    }

    public void setCategory(AlertCategory category) {
        this.category = category;
    }

    public AlertSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AlertSeverity severity) {
        this.severity = severity;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public Long getSourceValidationEventId() {
        return sourceValidationEventId;
    }

    public void setSourceValidationEventId(Long sourceValidationEventId) {
        this.sourceValidationEventId = sourceValidationEventId;
    }

    public int getObservedCount() {
        return observedCount;
    }

    public void setObservedCount(int observedCount) {
        this.observedCount = observedCount;
    }

    public boolean isEscalatedToControlCenter() {
        return escalatedToControlCenter;
    }

    public void setEscalatedToControlCenter(boolean escalatedToControlCenter) {
        this.escalatedToControlCenter = escalatedToControlCenter;
    }

    public OffsetDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(OffsetDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getLastOccurrenceAt() {
        return lastOccurrenceAt;
    }

    public void setLastOccurrenceAt(OffsetDateTime lastOccurrenceAt) {
        this.lastOccurrenceAt = lastOccurrenceAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getLastActionBy() {
        return lastActionBy;
    }

    public void setLastActionBy(String lastActionBy) {
        this.lastActionBy = lastActionBy;
    }

    public String getLastActionNote() {
        return lastActionNote;
    }

    public void setLastActionNote(String lastActionNote) {
        this.lastActionNote = lastActionNote;
    }

    public int getFalsePositiveCount() {
        return falsePositiveCount;
    }

    public void setFalsePositiveCount(int falsePositiveCount) {
        this.falsePositiveCount = falsePositiveCount;
    }
}