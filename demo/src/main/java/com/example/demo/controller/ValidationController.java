package com.example.demo.controller;

import com.example.demo.repository.ValidationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/validations")
public class ValidationController {

    @Autowired
    ValidationRepository repository;

    @GetMapping("/count-by-type")
    public List<Object[]> countByType() {
        return repository.countByTicketType();
    }

    @GetMapping("/insights")
    public Map<String, Object> getInsights() {
        List<Object[]> peakRows = repository.findPeakAfluencia();
        Object[] peak = (peakRows == null || peakRows.isEmpty()) ? null : peakRows.get(0);

        long total = repository.count();
        long invalidCount = repository.countByIsValidFalse();
        double invalidPercentage = total == 0 ? 0.0 : (invalidCount * 100.0) / total;

        String stopId = (peak != null && peak.length > 0) ? String.valueOf(peak[0]) : null;
        String stopName = (peak != null && peak.length > 1) ? String.valueOf(peak[1]) : null;
        String timeGap = (peak != null && peak.length > 2) ? String.valueOf(peak[2]) : null;
        long maxAfluencia = (peak != null && peak.length > 3 && peak[3] instanceof Number)
            ? ((Number) peak[3]).longValue()
            : 0L;
        long totalAtStop = (stopId != null) ? repository.countByStopId(stopId) : 0L;
        double peakAfluenciaPercentage = totalAtStop == 0 ? 0.0 : (maxAfluencia * 100.0) / totalAtStop;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stopId", stopId);
        response.put("stopName", stopName);
        response.put("timeGap", timeGap);
        response.put("peakAfluenciaPercentage", Math.round(peakAfluenciaPercentage * 100.0) / 100.0);
        response.put("invalidCount", invalidCount);
        response.put("invalidPercentage", Math.round(invalidPercentage * 100.0) / 100.0);
        return response;
    }
}