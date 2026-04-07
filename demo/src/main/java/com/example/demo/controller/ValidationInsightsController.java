package com.example.demo.controller;

import com.example.demo.service.ValidationInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/validations")
public class ValidationInsightsController {

    private final ValidationInsightsService validationInsightsService;

    public ValidationInsightsController(ValidationInsightsService validationInsightsService) {
        this.validationInsightsService = validationInsightsService;
    }

    @GetMapping("/insights")
    public Map<String, Object> obterInsights(@RequestParam(required = false) String stopId) {
        return validationInsightsService.obterInsights(Optional.ofNullable(stopId));
    }

    @GetMapping("/count-by-type")
    public List<Object[]> obterContagemPorTipo(@RequestParam(required = false) String stopId) {
        return validationInsightsService.obterContagemPorTipo(Optional.ofNullable(stopId));
    }
}
