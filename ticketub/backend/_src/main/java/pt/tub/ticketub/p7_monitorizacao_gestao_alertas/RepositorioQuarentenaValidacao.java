package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface RepositorioQuarentenaValidacao extends JpaRepository<QuarentenaValidacao, Long> {

    long countByCreatedAtAfter(OffsetDateTime reference);

    // UC06.3: contar inválidas por paragem
    long countByOriginStopId(String originStopId);

    // UC06.3: top 3 motivos de rejeição por paragem
    @Query("SELECT q.reason, COUNT(q) FROM QuarentenaValidacao q " +
           "WHERE q.originStopId = :stopId " +
           "GROUP BY q.reason ORDER BY COUNT(q) DESC")
    List<Object[]> findTopReasonsByOriginStopId(@Param("stopId") String stopId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM validation_quarantine WHERE raw_line LIKE :pattern OR received_value LIKE :pattern", nativeQuery = true)
    void deleteByCardIdPattern(@Param("pattern") String pattern);
}
