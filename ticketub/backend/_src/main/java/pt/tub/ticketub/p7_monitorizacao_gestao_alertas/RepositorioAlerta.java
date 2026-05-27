package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// O0.9.1.d – Repositório de Alertas e Anomalias (repositório)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface RepositorioAlerta extends JpaRepository<Alerta, Long> {
    List<Alerta> findByStatus(String status);
    List<Alerta> findByTypeAndStatus(String type, String status);
    long countByStatusAndCreatedAtAfter(String status, OffsetDateTime reference);
    long countByType(String type);
}
