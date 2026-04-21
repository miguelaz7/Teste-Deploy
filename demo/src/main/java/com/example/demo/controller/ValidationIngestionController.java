package com.example.demo.controller;

import com.example.demo.dto.ValidationIngestionBatchRequestDto;
import com.example.demo.dto.ValidationIngestionRequestDto;
import com.example.demo.dto.ValidationIngestionResponseDto;
import com.example.demo.service.ValidationIngestionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/validations")
public class ValidationIngestionController {

    private final ValidationIngestionService validationIngestionService;

    public ValidationIngestionController(ValidationIngestionService validationIngestionService) {
        this.validationIngestionService = validationIngestionService;
    }

    @PostMapping("/ingest")
    public ResponseEntity<ValidationIngestionResponseDto> ingerirValidacao(@RequestBody ValidationIngestionRequestDto validacao) {
        ValidationIngestionResponseDto resultado = validationIngestionService.ingerir(List.of(validacao));
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/ingest/batch")
    public ResponseEntity<ValidationIngestionResponseDto> ingerirLote(@RequestBody ValidationIngestionBatchRequestDto request) {
        ValidationIngestionResponseDto resultado = validationIngestionService.ingerir(request.getValidacoes());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}