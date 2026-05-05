package pt.tub.ticketub.p8_alertas_e_excecoes;

public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(String message) {
        super(message);
    }
}