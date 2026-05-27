package pt.tub.ticketub.p2_ingestao_processamento_dados;

// O0.2.2.d – Repositório de Dados Normalizados (entidade)

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.SistemaBilhetica;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Paragem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Viagem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "validation_events")
public class EventoValidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_id")
    private String cardId;

    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "ingestion_hash", nullable = false)
    private String ingestionHash;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TipoBilhete ticketType;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "transaction_datetime", nullable = false)
    private OffsetDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_stop_id")
    private Paragem originStop;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "trip_id")
    private String tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", referencedColumnName = "trip_id", insertable = false, updatable = false)
    private Viagem trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_collection_system_id")
    private SistemaBilhetica fareCollectionSystem;

    @Column(name = "fare_for_adult", precision = 10, scale = 2)
    private BigDecimal fareForAdult;

    @Column(name = "equipment_id")
    private String equipmentId;

    @Column(name = "transaction_vehicle_num")
    private String transactionVehicleNum;

    @Column(name = "result", nullable = false)
    private String result;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "perfil_classificado")
    private String perfilClassificado;

    @Column(name = "pii_detected")
    private Boolean piiDetected = false;

    @Column(name = "policy_version")
    private String policyVersion;

    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "masked_fields", length = 1000)
    private String maskedFields;

    public EventoValidacao() {}

    public Long getId()                                      { return id; }
    public String getCardId()                                { return cardId; }
    public void setCardId(String cardId)                     { this.cardId = cardId; }
    public String getTicketId()                              { return ticketId; }
    public void setTicketId(String ticketId)                 { this.ticketId = ticketId; }
    public String getIngestionHash()                         { return ingestionHash; }
    public void setIngestionHash(String ingestionHash)       { this.ingestionHash = ingestionHash; }
    public OffsetDateTime getIngestedAt()                    { return ingestedAt; }
    public void setIngestedAt(OffsetDateTime ingestedAt)     { this.ingestedAt = ingestedAt; }
    public String getMediaType()                             { return mediaType; }
    public void setMediaType(String mediaType)               { this.mediaType = mediaType; }
    public TipoBilhete getTicketType()                        { return ticketType; }
    public void setTicketType(TipoBilhete ticketType)         { this.ticketType = ticketType; }
    public String getTransactionType()                       { return transactionType; }
    public void setTransactionType(String t)                 { this.transactionType = t; }
    public OffsetDateTime getTransactionDateTime()           { return transactionDateTime; }
    public void setTransactionDateTime(OffsetDateTime t)     { this.transactionDateTime = t; }
    public Paragem getOriginStop()                              { return originStop; }
    public void setOriginStop(Paragem originStop)               { this.originStop = originStop; }
    public String getRouteId()                               { return routeId; }
    public void setRouteId(String routeId)                   { this.routeId = routeId; }
    public Viagem getTrip()                                    { return trip; }
    public String getTripId()                                { return tripId; }
    public void setTripId(String tripId)                     { this.tripId = tripId; }
    public SistemaBilhetica getFareCollectionSystem()    { return fareCollectionSystem; }
    public void setFareCollectionSystem(SistemaBilhetica f) { this.fareCollectionSystem = f; }
    public BigDecimal getFareForAdult()                      { return fareForAdult; }
    public void setFareForAdult(BigDecimal fareForAdult)     { this.fareForAdult = fareForAdult; }
    public String getEquipmentId()                           { return equipmentId; }
    public void setEquipmentId(String equipmentId)           { this.equipmentId = equipmentId; }
    public String getTransactionVehicleNum()                 { return transactionVehicleNum; }
    public void setTransactionVehicleNum(String t)           { this.transactionVehicleNum = t; }
    public String getResult()                                { return result; }
    public void setResult(String result)                     { this.result = result; }
    public String getRejectReason()                          { return rejectReason; }
    public void setRejectReason(String rejectReason)         { this.rejectReason = rejectReason; }
    public String getPerfilClassificado()                    { return perfilClassificado; }
    public void setPerfilClassificado(String p)              { this.perfilClassificado = p; }
    public Boolean getPiiDetected()                          { return piiDetected; }
    public void setPiiDetected(Boolean piiDetected)          { this.piiDetected = piiDetected; }

    public String getPolicyVersion()                         { return policyVersion; }
    public void setPolicyVersion(String policyVersion)       { this.policyVersion = policyVersion; }
    public Long getPolicyId()                                { return policyId; }
    public void setPolicyId(Long policyId)                   { this.policyId = policyId; }
    public String getMaskedFields()                          { return maskedFields; }
    public void setMaskedFields(String maskedFields)         { this.maskedFields = maskedFields; }
}
