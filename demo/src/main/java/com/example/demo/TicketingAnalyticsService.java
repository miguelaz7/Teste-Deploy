package com.example.demo;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TicketingAnalyticsService {

    private final ViagemRepository viagemRepository;
    private final ValidacaoBilheteRepository validacaoBilheteRepository;

    public TicketingAnalyticsService(ViagemRepository viagemRepository, ValidacaoBilheteRepository validacaoBilheteRepository) {
        this.viagemRepository = viagemRepository;
        this.validacaoBilheteRepository = validacaoBilheteRepository;
    }

    public List<RevenueByRouteResponse> getRevenueByRoute() {
        Map<String, BigDecimal> totals = new HashMap<>();

        for (ValidacaoBilhete validacao : validacaoBilheteRepository.findAll()) {
            String route = validacao.getViagem().getRota();
            totals.put(route, totals.getOrDefault(route, BigDecimal.ZERO).add(validacao.getValorPago()));
        }

        return totals.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(entry -> new RevenueByRouteResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<TripsByDayResponse> getTripsByDay() {
        Map<LocalDate, Long> counts = new HashMap<>();

        for (Viagem viagem : viagemRepository.findAll()) {
            LocalDate day = viagem.getDataHora().toLocalDate();
            counts.put(day, counts.getOrDefault(day, 0L) + 1L);
        }

        return counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new TripsByDayResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<TicketTypeDistributionResponse> getTicketTypeDistribution() {
        Map<String, Long> counts = new HashMap<>();

        for (ValidacaoBilhete validacao : validacaoBilheteRepository.findAll()) {
            String ticketType = validacao.getTipoBilhete();
            counts.put(ticketType, counts.getOrDefault(ticketType, 0L) + 1L);
        }

        return counts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(entry -> new TicketTypeDistributionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    public List<AnomalyResponse> getSimpleAnomalies() {
        List<AnomalyResponse> anomalies = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (ValidacaoBilhete validacao : validacaoBilheteRepository.findAll()) {
            boolean zeroPassengers = validacao.getNumeroPassageiros() != null && validacao.getNumeroPassageiros() == 0;
            boolean negativeValue = validacao.getValorPago() != null && validacao.getValorPago().compareTo(BigDecimal.ZERO) < 0;

            if (!zeroPassengers && !negativeValue) {
                continue;
            }

            String reason;
            if (zeroPassengers && negativeValue) {
                reason = "0 passageiros e valor negativo";
            } else if (zeroPassengers) {
                reason = "0 passageiros";
            } else {
                reason = "valor negativo";
            }

            Viagem viagem = validacao.getViagem();
                String signature = viagem.getViagemId()
                    + "|" + viagem.getDataHora()
                    + "|" + validacao.getNumeroPassageiros()
                    + "|" + normalizeDecimal(validacao.getValorPago())
                    + "|" + reason;

                if (!seen.add(signature)) {
                continue;
                }

            anomalies.add(new AnomalyResponse(
                    viagem.getViagemId(),
                    viagem.getRota(),
                    viagem.getDataHora(),
                    validacao.getNumeroPassageiros(),
                    validacao.getValorPago(),
                    reason
            ));
        }

        anomalies.sort(Comparator.comparing(AnomalyResponse::getDateTime));
        return anomalies;
    }

    private String normalizeDecimal(BigDecimal value) {
        if (value == null) {
            return "";
        }

        return value.stripTrailingZeros().toPlainString();
    }
}
