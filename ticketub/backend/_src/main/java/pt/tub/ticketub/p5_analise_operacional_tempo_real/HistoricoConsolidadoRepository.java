package pt.tub.ticketub.p5_analise_operacional_tempo_real;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoConsolidadoRepository extends JpaRepository<HistoricoConsolidado, Long> {
    List<HistoricoConsolidado> findByRouteId(String routeId);
    List<HistoricoConsolidado> findByGranularidade(String granularidade);
    Optional<HistoricoConsolidado> findByRouteIdAndPerfilTarifarioAndGranularidadeAndPeriodoInicio(
        String routeId, String perfilTarifario,
        String granularidade, LocalDate periodoInicio);
}
