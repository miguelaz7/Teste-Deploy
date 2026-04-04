package com.example.demo.model;

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
public class ValidationEvent {

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
    private TicketType ticketType;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "transaction_datetime", nullable = false)
    private OffsetDateTime transactionDateTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_stop_id")
    private Stop originStop;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "trip_id")
    private String tripId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fare_collection_system_id")
    private FareCollectionSystem fareCollectionSystem;

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

    public ValidationEvent() {
    }

    public Long getId() {
        return id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public String getTicketId() {
        return ticketId;
    }

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getIngestionHash() {
        return ingestionHash;
    }

    public void setIngestionHash(String ingestionHash) {
        this.ingestionHash = ingestionHash;
    }

    public OffsetDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(OffsetDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public void setTicketType(TicketType ticketType) {
        this.ticketType = ticketType;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public OffsetDateTime getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(OffsetDateTime transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public Stop getOriginStop() {
        return originStop;
    }

    public void setOriginStop(Stop originStop) {
        this.originStop = originStop;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public FareCollectionSystem getFareCollectionSystem() {
        return fareCollectionSystem;
    }

    public void setFareCollectionSystem(FareCollectionSystem fareCollectionSystem) {
        this.fareCollectionSystem = fareCollectionSystem;
    }

    public BigDecimal getFareForAdult() {
        return fareForAdult;
    }

    public void setFareForAdult(BigDecimal fareForAdult) {
        this.fareForAdult = fareForAdult;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getTransactionVehicleNum() {
        return transactionVehicleNum;
    }

    public void setTransactionVehicleNum(String transactionVehicleNum) {
        this.transactionVehicleNum = transactionVehicleNum;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(String rejectReason) {
        this.rejectReason = rejectReason;
    }
}