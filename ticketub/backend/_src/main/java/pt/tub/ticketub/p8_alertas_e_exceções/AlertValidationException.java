package pt.tub.ticketub.p8_alertas_e_excecoes;

public class AlertValidationException extends RuntimeException {

    public AlertValidationException(String message) {
        super(message);
    }
}