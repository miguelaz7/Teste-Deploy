package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioHorarioParagem extends JpaRepository<HorarioParagem, Long> {
    boolean existsByTripIdAndStopId(String tripId, String stopId);
}
