package pt.tub.ticketub.p2_ingestao;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ValidationIngestionRequestDto {

    private String cardId;
    private String ticketId;
    private String mediaType;
    private String ticketTypeCode;
    private String transactionType;
    private String transactionDateTime;
    private String originStopId;

    @JsonProperty("route_id")
    private String routeId;

    @JsonProperty("trip_id")
    private String tripId;

    private String fareForAdult;
    private String equipmentId;
    private String transactionVehicleNum;
    private String result;

    @JsonProperty("reject_reason")
    private String rejectReason;

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

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getTicketTypeCode() {
        return ticketTypeCode;
    }

    public void setTicketTypeCode(String ticketTypeCode) {
        this.ticketTypeCode = ticketTypeCode;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(String transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public String getOriginStopId() {
        return originStopId;
    }

    public void setOriginStopId(String originStopId) {
        this.originStopId = originStopId;
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

    public String getFareForAdult() {
        return fareForAdult;
    }

    public void setFareForAdult(String fareForAdult) {
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




