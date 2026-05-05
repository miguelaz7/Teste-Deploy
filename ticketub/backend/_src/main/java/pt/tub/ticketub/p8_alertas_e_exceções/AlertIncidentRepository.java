package pt.tub.ticketub.p8_alertas_e_excecoes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertIncidentRepository extends JpaRepository<AlertIncidentEntity, Long>, AlertIncidentStore {
}