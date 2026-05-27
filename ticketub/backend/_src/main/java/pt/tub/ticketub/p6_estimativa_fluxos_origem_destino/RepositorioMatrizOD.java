package pt.tub.ticketub.p6_estimativa_fluxos_origem_destino;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RepositorioMatrizOD extends JpaRepository<MatrizOD, Long> {
    List<MatrizOD> findByCalculationDate(LocalDate calculationDate);
    List<MatrizOD> findByCalculationDateAndVolumeGreaterThanEqual(LocalDate calculationDate, int limiar);
    boolean existsByCalculationDate(LocalDate calculationDate);
}
