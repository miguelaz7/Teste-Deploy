package pt.tub.ticketub.p9_interoperabilidade;

import pt.tub.ticketub.p9_interoperabilidade.StopTimes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StopTimesRepository extends JpaRepository<StopTimes, Long> {
	boolean existsByTripIdAndStopId(String tripId, String stopId);
}






