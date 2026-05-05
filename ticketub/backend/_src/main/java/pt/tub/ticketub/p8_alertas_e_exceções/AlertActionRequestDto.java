package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlertActionRequestDto(
    @NotNull AlertActionType actionType,
    @NotBlank String actor,
    String notes
) {
}