package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface ValidationQuarantineRepository extends JpaRepository<ValidationQuarantine, Long> {
	long countByCreatedAtAfter(OffsetDateTime reference);
}






