package pt.tub.ticketub.p3_categorizacao;

import pt.tub.ticketub.p3_categorizacao.CategorizationStatsDto;
import pt.tub.ticketub.p3_categorizacao.CategorizationAudit;
import pt.tub.ticketub.p3_categorizacao.TipologiaPerfilMapping;
import pt.tub.ticketub.p2_ingestao.ValidationEvent;
import pt.tub.ticketub.p3_categorizacao.CategorizationService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categorization")
public class CategorizationController {

    private final CategorizationService service;

    public CategorizationController(CategorizationService service) {
        this.service = service;
    }

    @GetMapping("/stats")
    public ResponseEntity<CategorizationStatsDto> getStats() {
        return ResponseEntity.ok(service.getStats());
    }

    @GetMapping("/mappings")
    public ResponseEntity<List<TipologiaPerfilMapping>> getMappings() {
        return ResponseEntity.ok(service.getMappings());
    }

    @PostMapping("/mappings")
    public ResponseEntity<TipologiaPerfilMapping> createMapping(@RequestBody TipologiaPerfilMapping mapping) {
        return ResponseEntity.ok(service.createMapping(mapping));
    }

    @PutMapping("/mappings/{id}")
    public ResponseEntity<TipologiaPerfilMapping> updateMapping(
            @PathVariable Long id,
            @RequestBody TipologiaPerfilMapping mapping) {
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

    @GetMapping("/uncategorized")
    public ResponseEntity<List<ValidationEvent>> getUncategorized(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(service.getUncategorized(dataInicio, dataFim));
    }

    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, String>> reprocess() {
        service.reprocess();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Reprocessamento concluído."));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        service.reset();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Sistema limpo com sucesso."));
    }

    @DeleteMapping("/uncategorized")
    public ResponseEntity<Void> deleteUncategorized() {
        service.deleteUncategorized();
        return ResponseEntity.noContent().build();
    }
}






