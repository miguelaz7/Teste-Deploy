package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdDataLakeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NgsiLdDataLakeRecordRepository extends JpaRepository<NgsiLdDataLakeRecord, Long> {
    long countByBatchId(String batchId);
}






