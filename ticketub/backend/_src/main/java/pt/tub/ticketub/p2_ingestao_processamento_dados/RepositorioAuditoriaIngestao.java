package pt.tub.ticketub.p2_ingestao_processamento_dados;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;

@Repository
public interface RepositorioAuditoriaIngestao extends JpaRepository<RegistoAuditoriaIngestao, Long> {
    long countByEventTypeInAndCreatedAtAfter(Collection<String> eventTypes, OffsetDateTime reference);
}
