package pt.tub.ticketub.p2_ingestao;

import pt.tub.ticketub.p2_ingestao.IngestionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Collection;

public interface IngestionAuditLogRepository extends JpaRepository<IngestionAuditLog, Long> {
	long countByEventTypeInAndCreatedAtAfter(Collection<String> eventTypes, OffsetDateTime reference);
}






