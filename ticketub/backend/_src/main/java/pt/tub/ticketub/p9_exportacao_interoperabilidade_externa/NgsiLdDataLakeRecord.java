package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
    name = "ngsi_ld_data_lake",
    indexes = {
        @Index(name = "idx_datalake_batch_id", columnList = "batch_id"),
        @Index(name = "idx_datalake_partition_date", columnList = "partition_date"),
        @Index(name = "idx_datalake_temporal", columnList = "observation_datetime"),
        @Index(name = "idx_datalake_geospatial", columnList = "geo_lat,geo_lon")
    }
)
public class NgsiLdDataLakeRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Lob
    @Column(name = "payload_json", nullable = false, columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "partition_date", nullable = false)
    private LocalDate partitionDate;

    @Column(name = "observation_datetime")
    private OffsetDateTime observationDateTime;

    @Column(name = "geo_lat")
    private Double geoLat;

    @Column(name = "geo_lon")
    private Double geoLon;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public NgsiLdDataLakeRecord() {
    }

    public NgsiLdDataLakeRecord(
        String batchId,
        String entityId,
        String entityType,
        String payloadJson,
        LocalDate partitionDate,
        OffsetDateTime observationDateTime,
        Double geoLat,
        Double geoLon,
        OffsetDateTime createdAt
    ) {
        this.batchId = batchId;
        this.entityId = entityId;
        this.entityType = entityType;
        this.payloadJson = payloadJson;
        this.partitionDate = partitionDate;
        this.observationDateTime = observationDateTime;
        this.geoLat = geoLat;
        this.geoLon = geoLon;
        this.createdAt = createdAt;
    }
}





