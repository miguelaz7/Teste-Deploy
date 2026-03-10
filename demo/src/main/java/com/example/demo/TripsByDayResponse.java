package com.example.demo;

import java.time.LocalDate;

public class TripsByDayResponse {

    private final LocalDate day;
    private final long trips;

    public TripsByDayResponse(LocalDate day, long trips) {
        this.day = day;
        this.trips = trips;
    }

    public LocalDate getDay() {
        return day;
    }

    public long getTrips() {
        return trips;
    }
}
