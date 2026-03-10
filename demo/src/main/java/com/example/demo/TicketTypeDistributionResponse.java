package com.example.demo;

public class TicketTypeDistributionResponse {

    private final String ticketType;
    private final long count;

    public TicketTypeDistributionResponse(String ticketType, long count) {
        this.ticketType = ticketType;
        this.count = count;
    }

    public String getTicketType() {
        return ticketType;
    }

    public long getCount() {
        return count;
    }
}
