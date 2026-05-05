package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AlertConfigurationRequestDto(
    @NotNull @Min(1) Integer detectionWindowMinutes,
    @NotNull @Min(2) Integer repeatCardThreshold,
    @NotNull @Min(1) Integer volumeSpikeThreshold,
    @NotNull @DecimalMin(value = "0.0", inclusive = true) @DecimalMax(value = "1.0", inclusive = true) Double criticalInvalidRatio,
    @NotNull @Min(1) Integer minEventsForRatio
) {
}