package pt.tub.ticketub.p8_alertas_e_excecoes;

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;

import java.time.OffsetDateTime;
import java.util.List;

public interface ValidationEventAlertSource {
    List<ValidationEvent> findByTransactionDateTimeAfter(OffsetDateTime reference);
}