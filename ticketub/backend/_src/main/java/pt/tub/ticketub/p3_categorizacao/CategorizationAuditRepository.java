package pt.tub.ticketub.p3_categorizacao;

import pt.tub.ticketub.p3_categorizacao.CategorizationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategorizationAuditRepository extends JpaRepository<CategorizationAudit, Long> {

}






