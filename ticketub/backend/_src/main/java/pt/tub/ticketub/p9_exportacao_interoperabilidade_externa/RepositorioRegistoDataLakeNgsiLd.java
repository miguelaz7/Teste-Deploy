package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioRegistoDataLakeNgsiLd extends JpaRepository<RegistoDataLakeNgsiLd, Long> {
    long countByBatchId(String batchId);
    java.util.Optional<RegistoDataLakeNgsiLd> findFirstByEntityId(String entityId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM ngsi_ld_data_lake WHERE payload_json LIKE :pattern", nativeQuery = true)
    void deleteByCardIdPattern(@org.springframework.data.repository.query.Param("pattern") String pattern);
}
