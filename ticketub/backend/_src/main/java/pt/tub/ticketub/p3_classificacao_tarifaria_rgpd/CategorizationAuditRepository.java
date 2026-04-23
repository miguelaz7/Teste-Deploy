package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import pt.tub.ticketub.p3_classificacao_tarifaria_rgpd.CategorizationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategorizationAuditRepository extends JpaRepository<CategorizationAudit, Long> {

}






