package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertStatusUpdateRequestDto(
    @NotNull AlertStatus status,
    @NotBlank String actor,
    String notes
) {
}