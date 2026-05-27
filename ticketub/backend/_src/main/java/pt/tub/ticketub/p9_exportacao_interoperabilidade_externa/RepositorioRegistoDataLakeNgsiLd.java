package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioRegistoDataLakeNgsiLd extends JpaRepository<RegistoDataLakeNgsiLd, Long> {
    long countByBatchId(String batchId);
}
