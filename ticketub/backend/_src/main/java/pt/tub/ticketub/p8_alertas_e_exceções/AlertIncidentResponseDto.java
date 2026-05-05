package pt.tub.ticketub.p8_alertas_e_excecoes;

import java.time.OffsetDateTime;

public record AlertIncidentResponseDto(
    Long id,
    String alertFingerprint,
    AlertCategory category,
    AlertSeverity severity,
    AlertStatus status,
    String title,
    String description,
    String routeId,
    String stopId,
    String tripId,
    String cardId,
    String ticketId,
    Long sourceValidationEventId,
    int observedCount,
    boolean escalatedToControlCenter,
    OffsetDateTime detectedAt,
    OffsetDateTime updatedAt,
    OffsetDateTime lastOccurrenceAt,
    OffsetDateTime resolvedAt,
    String assignedTo,
    String lastActionBy,
    String lastActionNote,
    int falsePositiveCount
) {

    public static AlertIncidentResponseDto fromEntity(AlertIncidentEntity entity) {
        return new AlertIncidentResponseDto(
            entity.getId(),
            entity.getAlertFingerprint(),
            entity.getCategory(),
            entity.getSeverity(),
            entity.getStatus(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getRouteId(),
            entity.getStopId(),
            entity.getTripId(),
            entity.getCardId(),
            entity.getTicketId(),
            entity.getSourceValidationEventId(),
            entity.getObservedCount(),
            entity.isEscalatedToControlCenter(),
            entity.getDetectedAt(),
            entity.getUpdatedAt(),
            entity.getLastOccurrenceAt(),
            entity.getResolvedAt(),
            entity.getAssignedTo(),
            entity.getLastActionBy(),
            entity.getLastActionNote(),
            entity.getFalsePositiveCount()
        );
    }
}