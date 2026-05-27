package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RepositorioAuditoriaCategorizacao extends JpaRepository<AuditoriaCategorizacao, Long> {
}
