package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopTimes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StopTimesRepository extends JpaRepository<StopTimes, Long> {
	boolean existsByTripIdAndStopId(String tripId, String stopId);
}






