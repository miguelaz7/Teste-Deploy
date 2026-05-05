package pt.tub.ticketub.p8_alertas_e_excecoes;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
    OffsetDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {
}