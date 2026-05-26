package pt.tub.ticketub.p6_estimativa_fluxos_origem_destino;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MatrizODRepository extends JpaRepository<MatrizOD, Long> {
    List<MatrizOD> findByDataCalculo(LocalDate dataCalculo);
    List<MatrizOD> findByDataCalculoAndVolumeGreaterThanEqual(LocalDate dataCalculo, int limiar);
    boolean existsByDataCalculo(LocalDate dataCalculo);
}
