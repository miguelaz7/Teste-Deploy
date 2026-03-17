package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "validations")
public class Validation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private String stopId;

    @Column(nullable = false)
    private String stopName;

    @Column(nullable = false)
    private Double stopLat;

    @Column(nullable = false)
    private Double stopLon;

    @Column(nullable = false)
    private String ticketType;

    @Column(nullable = false)
    private Boolean isValid;

    // Construtor vazio (obrigatório para JPA)
    public Validation() {
    }

    // Construtor completo (opcional mas útil)
    public Validation(LocalDateTime timestamp, String stopId, String stopName,
                      Double stopLat, Double stopLon, String ticketType, Boolean isValid) {
        this.timestamp = timestamp;
        this.stopId = stopId;
        this.stopName = stopName;
        this.stopLat = stopLat;
        this.stopLon = stopLon;
        this.ticketType = ticketType;
        this.isValid = isValid;
    }

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getStopId() {
        return stopId;
    }

    public void setStopId(String stopId) {
        this.stopId = stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public void setStopName(String stopName) {
        this.stopName = stopName;
    }

    public Double getStopLat() {
        return stopLat;
    }

    public void setStopLat(Double stopLat) {
        this.stopLat = stopLat;
    }

    public Double getStopLon() {
        return stopLon;
    }

    public void setStopLon(Double stopLon) {
        this.stopLon = stopLon;
    }

    public String getTicketType() {
        return ticketType;
    }

    public void setTicketType(String ticketType) {
        this.ticketType = ticketType;
    }

    public Boolean getIsValid() {
        return isValid;
    }

    public void setIsValid(Boolean isValid) {
        this.isValid = isValid;
    }
}