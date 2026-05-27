package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RepositorioExportacaoDadosAbertos extends JpaRepository<ExportacaoDadosAbertos, Long> {
    List<ExportacaoDadosAbertos> findByStatus(String status);
    List<ExportacaoDadosAbertos> findByRequestedBy(String requestedBy);
}
