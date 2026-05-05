package pt.tub.ticketub.p8_alertas_e_excecoes;

import java.util.Optional;

public interface AlertConfigurationStore {
    Optional<AlertConfigurationEntity> findTopByOrderByIdAsc();

    AlertConfigurationEntity save(AlertConfigurationEntity alertConfigurationEntity);
}