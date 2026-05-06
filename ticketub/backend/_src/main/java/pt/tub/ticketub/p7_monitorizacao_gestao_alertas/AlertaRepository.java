package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// O0.9.1.d – Repositório de Alertas e Anomalias (repositório)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByEstado(String estado);
    List<Alerta> findByTipoAndEstado(String tipo, String estado);
    long countByEstadoAndCriadoEmAfter(String estado, OffsetDateTime reference);
    long countByTipo(String tipo);
}
