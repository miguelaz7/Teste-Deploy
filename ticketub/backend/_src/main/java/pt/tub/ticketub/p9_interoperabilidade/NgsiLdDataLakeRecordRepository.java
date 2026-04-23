package pt.tub.ticketub.p9_interoperabilidade;

import pt.tub.ticketub.p9_interoperabilidade.NgsiLdDataLakeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NgsiLdDataLakeRecordRepository extends JpaRepository<NgsiLdDataLakeRecord, Long> {
    long countByBatchId(String batchId);
}






