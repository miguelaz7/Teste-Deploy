package pt.tub.ticketub.p2_ingestao_processamento_dados;

// O0.2.2.d – Repositório de Dados Normalizados (repositório)

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ValidationEventRepository extends JpaRepository<ValidationEvent, Long> {

    boolean existsByIngestionHashAndIngestedAtAfter(String ingestionHash, OffsetDateTime reference);

    List<ValidationEvent> findByOriginStop_StopId(String stopId);

    long countByIngestedAtAfter(OffsetDateTime reference);

    long countByPerfilClassificado(String perfilClassificado);

    List<ValidationEvent> findByPerfilClassificado(String perfilClassificado);

    List<ValidationEvent> findByPerfilClassificadoIsNullOrPerfilClassificado(String perfilClassificado);

    List<ValidationEvent> findByPerfilClassificadoAndTransactionDateTimeBetween(
        String perfilClassificado, OffsetDateTime inicio, OffsetDateTime fim);

    List<ValidationEvent> findByTicketType_Code(String code);

    @Query("SELECT e.routeId, COUNT(e) FROM ValidationEvent e WHERE e.ingestedAt > :reference GROUP BY e.routeId")
    List<Object[]> countByRouteIdAfter(@Param("reference") OffsetDateTime reference);

    @Query("SELECT e.ticketType.code, COUNT(e) FROM ValidationEvent e WHERE e.ingestedAt > :reference GROUP BY e.ticketType.code")
    List<Object[]> countByTicketTypeAfter(@Param("reference") OffsetDateTime reference);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE validation_events SET perfil_classificado = :perfil", nativeQuery = true)
    void resetAllClassifications(@Param("perfil") String perfil);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE validation_events e JOIN ticket_types t ON e.ticket_type_id = t.id SET e.perfil_classificado = :perfil WHERE t.code = :code", nativeQuery = true)
    void updateProfileByTicketCode(@Param("code") String code, @Param("perfil") String perfil);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM validation_events WHERE perfil_classificado = :perfil", nativeQuery = true)
    void deleteByPerfilClassificado(@Param("perfil") String perfil);
}
