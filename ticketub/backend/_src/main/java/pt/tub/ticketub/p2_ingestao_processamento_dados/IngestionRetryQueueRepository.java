package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p2_ingestao_processamento_dados.IngestionRetryQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface IngestionRetryQueueRepository extends JpaRepository<IngestionRetryQueue, Long> {
	List<IngestionRetryQueue> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(String status, OffsetDateTime reference);
}






