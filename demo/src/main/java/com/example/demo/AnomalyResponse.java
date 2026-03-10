package com.example.demo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AnomalyResponse {

    private final String viagemId;
    private final String route;
    private final LocalDateTime dateTime;
    private final Integer passengers;
    private final BigDecimal paidValue;
    private final String reason;

    public AnomalyResponse(String viagemId, String route, LocalDateTime dateTime, Integer passengers, BigDecimal paidValue, String reason) {
        this.viagemId = viagemId;
        this.route = route;
        this.dateTime = dateTime;
        this.passengers = passengers;
        this.paidValue = paidValue;
        this.reason = reason;
    }

    public String getViagemId() {
        return viagemId;
    }

    public String getRoute() {
        return route;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public Integer getPassengers() {
        return passengers;
    }

    public BigDecimal getPaidValue() {
        return paidValue;
    }

    public String getReason() {
        return reason;
    }
}
