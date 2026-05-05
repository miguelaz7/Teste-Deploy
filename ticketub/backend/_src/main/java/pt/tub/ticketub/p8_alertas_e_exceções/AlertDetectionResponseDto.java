package pt.tub.ticketub.p8_alertas_e_excecoes;

import java.time.OffsetDateTime;
import java.util.List;

public record AlertDetectionResponseDto(
    OffsetDateTime generatedAt,
    int analysedEvents,
    int createdAlerts,
    int updatedAlerts,
    int escalatedAlerts,
    List<AlertIncidentResponseDto> alerts
) {
}