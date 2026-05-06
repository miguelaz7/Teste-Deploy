package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O0.7.1.d – Repositório de Correlações GPS
// Resultado do cruzamento de cada validação com o GPS: paragem associada
// e indicador de qualidade (exacta / estimada / por horário planeado).
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "correlacoes_gps")
class CorrelacaoGPS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ingestion_hash", nullable = false)
    private String ingestionHash;

    @Column(name = "stop_id_correlacionado")
    private String stopIdCorrelacionado;

    @Column(name = "route_id")
    private String routeId;

    // Qualidade: EXACTA, ESTIMADA, HORARIO_PLANEADO
    @Column(name = "indicador_qualidade", nullable = false)
    private String indicadorQualidade;

    @Column(name = "latitude_gps")
    private Double latitudeGps;

    @Column(name = "longitude_gps")
    private Double longitudeGps;

    @Column(name = "correlacionado_em", nullable = false)
    private OffsetDateTime correlacionadoEm;

    CorrelacaoGPS() {}

    CorrelacaoGPS(String ingestionHash, String stopIdCorrelacionado, String routeId,
                  String indicadorQualidade, Double latitudeGps, Double longitudeGps,
                  OffsetDateTime correlacionadoEm) {
        this.ingestionHash        = ingestionHash;
        this.stopIdCorrelacionado = stopIdCorrelacionado;
        this.routeId              = routeId;
        this.indicadorQualidade   = indicadorQualidade;
        this.latitudeGps          = latitudeGps;
        this.longitudeGps         = longitudeGps;
        this.correlacionadoEm     = correlacionadoEm;
    }

    Long getId()                      { return id; }
    String getIngestionHash()         { return ingestionHash; }
    String getStopIdCorrelacionado()  { return stopIdCorrelacionado; }
    String getRouteId()               { return routeId; }
    String getIndicadorQualidade()    { return indicadorQualidade; }
    Double getLatitudeGps()           { return latitudeGps; }
    Double getLongitudeGps()          { return longitudeGps; }
    OffsetDateTime getCorrelacionadoEm() { return correlacionadoEm; }
}

@Repository
interface CorrelacaoGPSRepository extends JpaRepository<CorrelacaoGPS, Long> {
    List<CorrelacaoGPS> findByRouteId(String routeId);
    long countByIndicadorQualidade(String indicadorQualidade);
    boolean existsByIngestionHash(String ingestionHash);
}
