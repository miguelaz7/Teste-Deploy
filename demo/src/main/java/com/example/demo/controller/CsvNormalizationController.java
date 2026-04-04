package com.example.demo.controller;

import com.example.demo.dto.CsvImportResultDto;
import com.example.demo.service.CsvNormalizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/importacao")
public class CsvNormalizationController {

    private final CsvNormalizationService csvNormalizationService;

    public CsvNormalizationController(CsvNormalizationService csvNormalizationService) {
        this.csvNormalizationService = csvNormalizationService;
    }

    @PostMapping("/validacoes/normalizar")
    public ResponseEntity<CsvImportResultDto> normalizarValidacoes() throws Exception {
        CsvImportResultDto resultado = csvNormalizationService.importarCsvNormalizado();
        return ResponseEntity.ok(resultado);
    }
}