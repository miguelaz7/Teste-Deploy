package pt.tub.ticketub.p2_ingestao_processamento_dados;

// O0.2.4.d – Repositório de Estatísticas de Ingestão (repositório de estatísticas)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IngestionBatchStatsRepository extends JpaRepository<IngestionBatchStats, Long> {
}
