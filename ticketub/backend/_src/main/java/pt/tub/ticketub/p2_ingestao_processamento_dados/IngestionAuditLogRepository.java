package pt.tub.ticketub.p2_ingestao_processamento_dados;

// O0.2.4.d – Repositório de Estatísticas de Ingestão (repositório de auditoria)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;

@Repository
public interface IngestionAuditLogRepository extends JpaRepository<IngestionAuditLog, Long> {
    long countByEventTypeInAndCreatedAtAfter(Collection<String> eventTypes, OffsetDateTime reference);
}
