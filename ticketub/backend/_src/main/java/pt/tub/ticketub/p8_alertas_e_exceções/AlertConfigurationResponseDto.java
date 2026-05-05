package pt.tub.ticketub.p8_alertas_e_excecoes;

public record AlertConfigurationResponseDto(
    Long id,
    int detectionWindowMinutes,
    int repeatCardThreshold,
    int volumeSpikeThreshold,
    double criticalInvalidRatio,
    int minEventsForRatio
) {

    public static AlertConfigurationResponseDto fromEntity(AlertConfigurationEntity entity) {
        return new AlertConfigurationResponseDto(
            entity.getId(),
            entity.getDetectionWindowMinutes(),
            entity.getRepeatCardThreshold(),
            entity.getVolumeSpikeThreshold(),
            entity.getCriticalInvalidRatio(),
            entity.getMinEventsForRatio()
        );
    }
}