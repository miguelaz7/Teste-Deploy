package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TripRepository extends JpaRepository<Trip, String> {
}






