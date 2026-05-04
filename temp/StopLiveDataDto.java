package pt.tub.ticketub.p4_interfaces_utilizador;

import java.util.Map;

public class StopLiveDataDto {

    private final String stopId;
    private final String stopName;
    private final String peakHourWindowStart;
    private final String peakHourWindowEnd;
    private final long peakHourValidationsCount;
    private final double peakHourValidationsPercentageOfStopTotal;
    private final long invalidValidationsCount;
    private final double invalidValidationsPercentageOfStopTotal;
    private final Map<String, Long> ticketTypeDistribution;
    private final long totalValidations;

    public StopLiveDataDto(
        String stopId,
        String stopName,
        String peakHourWindowStart,
        String peakHourWindowEnd,
        long peakHourValidationsCount,
        double peakHourValidationsPercentageOfStopTotal,
        long invalidValidationsCount,
        double invalidValidationsPercentageOfStopTotal,
        Map<String, Long> ticketTypeDistribution,
        long totalValidations
    ) {
        this.stopId = stopId;
        this.stopName = stopName;
        this.peakHourWindowStart = peakHourWindowStart;
        this.peakHourWindowEnd = peakHourWindowEnd;
        this.peakHourValidationsCount = peakHourValidationsCount;
        this.peakHourValidationsPercentageOfStopTotal = peakHourValidationsPercentageOfStopTotal;
        this.invalidValidationsCount = invalidValidationsCount;
        this.invalidValidationsPercentageOfStopTotal = invalidValidationsPercentageOfStopTotal;
        this.ticketTypeDistribution = ticketTypeDistribution;
        this.totalValidations = totalValidations;
    }

    public String getStopId() {
        return stopId;
    }

    public String getStopName() {
        return stopName;
    }

    public String getPeakHourWindowStart() {
        return peakHourWindowStart;
    }

    public String getPeakHourWindowEnd() {
        return peakHourWindowEnd;
    }

    public long getPeakHourValidationsCount() {
        return peakHourValidationsCount;
    }

    public double getPeakHourValidationsPercentageOfStopTotal() {
        return peakHourValidationsPercentageOfStopTotal;
    }

    public long getInvalidValidationsCount() {
        return invalidValidationsCount;
    }

    public double getInvalidValidationsPercentageOfStopTotal() {
        return invalidValidationsPercentageOfStopTotal;
    }

    public Map<String, Long> getTicketTypeDistribution() {
        return ticketTypeDistribution;
    }

    public long getTotalValidations() {
        return totalValidations;
    }
}





