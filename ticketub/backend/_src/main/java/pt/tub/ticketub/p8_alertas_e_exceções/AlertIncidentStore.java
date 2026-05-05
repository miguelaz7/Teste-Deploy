package pt.tub.ticketub.p8_alertas_e_excecoes;

import java.util.List;
import java.util.Optional;

public interface AlertIncidentStore {
    AlertIncidentEntity save(AlertIncidentEntity alertIncidentEntity);

    Optional<AlertIncidentEntity> findById(Long id);

    List<AlertIncidentEntity> findAll();

    List<AlertIncidentEntity> findAllByAlertFingerprintOrderByDetectedAtDesc(String alertFingerprint);
}