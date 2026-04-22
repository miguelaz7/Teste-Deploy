package com.example.demo.repository;

import com.example.demo.model.ValidationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface ValidationEventRepository extends JpaRepository<ValidationEvent, Long> {

    boolean existsByIngestionHashAndIngestedAtAfter(String ingestionHash, OffsetDateTime reference);

    List<ValidationEvent> findByOriginStop_StopId(String stopId);

    long countByIngestedAtAfter(OffsetDateTime reference);

    // UC03 — categorização
    long countByPerfilClassificado(String perfilClassificado);

    List<ValidationEvent> findByPerfilClassificado(String perfilClassificado);

    List<ValidationEvent> findByPerfilClassificadoIsNullOrPerfilClassificado(String perfilClassificado);

    List<ValidationEvent> findByPerfilClassificadoAndTransactionDateTimeBetween(
            String perfilClassificado, OffsetDateTime inicio, OffsetDateTime fim);

    List<ValidationEvent> findByTicketType_Code(String code);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE validation_events SET perfil_classificado = :perfil", nativeQuery = true)
    void resetAllClassifications(String perfil);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "UPDATE validation_events e JOIN ticket_types t ON e.ticket_type_id = t.id SET e.perfil_classificado = :perfil WHERE t.code = :code", nativeQuery = true)
    void updateProfileByTicketCode(String code, String perfil);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM validation_events WHERE perfil_classificado = :perfil", nativeQuery = true)
    void deleteByPerfilClassificado(String perfil);
}