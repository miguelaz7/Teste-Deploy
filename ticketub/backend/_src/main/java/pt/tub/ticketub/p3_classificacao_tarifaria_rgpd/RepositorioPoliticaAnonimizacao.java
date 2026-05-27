package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RepositorioPoliticaAnonimizacao extends JpaRepository<PoliticaAnonimizacao, Long> {
    List<PoliticaAnonimizacao> findByStatus(String status);
    List<PoliticaAnonimizacao> findByField(String field);
}
