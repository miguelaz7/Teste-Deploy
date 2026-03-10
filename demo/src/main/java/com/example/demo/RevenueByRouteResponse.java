package com.example.demo;

import java.math.BigDecimal;

public class RevenueByRouteResponse {

    private final String route;
    private final BigDecimal totalRevenue;

    public RevenueByRouteResponse(String route, BigDecimal totalRevenue) {
        this.route = route;
        this.totalRevenue = totalRevenue;
    }

    public String getRoute() {
        return route;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }
}
