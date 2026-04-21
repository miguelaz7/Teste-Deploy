package com.example.demo.controller;

import com.example.demo.model.CategorizationAudit;
import com.example.demo.model.TipologiaPerfilMapping;
import com.example.demo.model.ValidationEvent;
import com.example.demo.service.CategorizationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorization")
public class CategorizationController {

    private final CategorizationService service;

    public CategorizationController(CategorizationService service) {
        this.service = service;
    }

    @GetMapping("/mappings")
    public ResponseEntity<List<TipologiaPerfilMapping>> getMappings() {
        return ResponseEntity.ok(service.getAllMappings());
    }

    @PostMapping("/mappings")
    public ResponseEntity<TipologiaPerfilMapping> createMapping(@RequestBody TipologiaPerfilMapping mapping) {
        return ResponseEntity.ok(service.createMapping(mapping));
    }

    @PutMapping("/mappings/{id}")
    public ResponseEntity<TipologiaPerfilMapping> updateMapping(@PathVariable Long id, @RequestBody TipologiaPerfilMapping mapping) {
        return ResponseEntity.ok(service.updateMapping(id, mapping));
    }

    @DeleteMapping("/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        service.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/audit")
    public ResponseEntity<CategorizationAudit> logAudit(@RequestBody CategorizationAudit audit) {
        return ResponseEntity.ok(service.logAudit(audit));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/uncategorized")
    public ResponseEntity<List<ValidationEvent>> getUncategorized(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {
        return ResponseEntity.ok(service.getUncategorized(startDate, endDate));
    }

    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, String>> reprocess() {
        service.reprocess();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Reprocessing completed successfully."));
    }
}
