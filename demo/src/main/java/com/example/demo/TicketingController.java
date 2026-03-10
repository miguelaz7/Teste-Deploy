package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ticketing")
@CrossOrigin(origins = "*")
public class TicketingController {

    private final TicketingImportService ticketingImportService;
    private final TicketingAnalyticsService ticketingAnalyticsService;
    private final String fixedFilePath;

    public TicketingController(
            TicketingImportService ticketingImportService,
            TicketingAnalyticsService ticketingAnalyticsService,
            @Value("${app.ticketing.fixed-file-path}") String fixedFilePath
    ) {
        this.ticketingImportService = ticketingImportService;
        this.ticketingAnalyticsService = ticketingAnalyticsService;
        this.fixedFilePath = fixedFilePath;
    }

    @PostMapping("/import")
    public ImportResultResponse importFixedFile() {
        return ticketingImportService.importFromPath(fixedFilePath);
    }

    @GetMapping("/analytics/revenue-by-route")
    public List<RevenueByRouteResponse> revenueByRoute() {
        return ticketingAnalyticsService.getRevenueByRoute();
    }

    @GetMapping("/analytics/trips-by-day")
    public List<TripsByDayResponse> tripsByDay() {
        return ticketingAnalyticsService.getTripsByDay();
    }

    @GetMapping("/analytics/ticket-types")
    public List<TicketTypeDistributionResponse> ticketTypeDistribution() {
        return ticketingAnalyticsService.getTicketTypeDistribution();
    }

    @GetMapping("/analytics/anomalies")
    public List<AnomalyResponse> anomalies() {
        return ticketingAnalyticsService.getSimpleAnomalies();
    }
}
