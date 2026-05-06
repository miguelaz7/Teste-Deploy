package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

// =============================================================================
// O0.3.3.i – Interface de Revisão de Eventos
// Interface para o Administrador de IT consultar a fila de eventos não
// categorizados (O0.3.3.d) e actualizar a tabela de mapeamento (O0.3.1.d).
// =============================================================================

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categorizacao")
public class InterfaceRevisaoEventos {

    private final ControladorClassificacaoTarifaria controlador;
    private final EventoNaoCategorizadoRepository eventoNaoCategorizadoRepository;

    public InterfaceRevisaoEventos(
        ControladorClassificacaoTarifaria controlador,
        EventoNaoCategorizadoRepository eventoNaoCategorizadoRepository
    ) {
        this.controlador                  = controlador;
        this.eventoNaoCategorizadoRepository = eventoNaoCategorizadoRepository;
    }

    // Estatísticas de categorização
    @GetMapping("/stats")
    public ResponseEntity<CategorizationStatsDto> getStats() {
        return ResponseEntity.ok(controlador.getStats());
    }

    // Lista mapeamentos tarifários
    @GetMapping("/mappings")
    public ResponseEntity<List<TipologiaPerfilMapping>> getMappings() {
        return ResponseEntity.ok(controlador.getMappings());
    }

    // Cria novo mapeamento
    @PostMapping("/mappings")
    public ResponseEntity<TipologiaPerfilMapping> createMapping(@RequestBody TipologiaPerfilMapping mapping) {
        return ResponseEntity.ok(controlador.createMapping(mapping));
    }

    // Actualiza mapeamento existente
    @PutMapping("/mappings/{id}")
    public ResponseEntity<TipologiaPerfilMapping> updateMapping(
        @PathVariable Long id, @RequestBody TipologiaPerfilMapping mapping
    ) {
        return ResponseEntity.ok(controlador.updateMapping(id, mapping));
    }

    // Apaga mapeamento
    @DeleteMapping("/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        controlador.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }

    // Fila de eventos não categorizados (O0.3.3.d)
    @GetMapping("/nao-categorizados")
    public ResponseEntity<List<EventoNaoCategorizado>> getNaoCategorizados() {
        return ResponseEntity.ok(eventoNaoCategorizadoRepository.findByEstado("PENDENTE"));
    }

    // Eventos de validação não categorizados com filtro de data
    @GetMapping("/uncategorized")
    public ResponseEntity<List<ValidationEvent>> getUncategorized(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim
    ) {
        return ResponseEntity.ok(controlador.getUncategorized(dataInicio, dataFim));
    }

    // Resolve um evento não categorizado (reclassifica ou rejeita)
    @PutMapping("/nao-categorizados/{id}/resolver")
    public ResponseEntity<EventoNaoCategorizado> resolverEvento(
        @PathVariable Long id, @RequestBody Map<String, String> body
    ) {
        EventoNaoCategorizado evento = eventoNaoCategorizadoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Evento não encontrado: " + id));
        evento.setEstado(body.getOrDefault("estado", "RECLASSIFICADO"));
        evento.setResolvidoEm(OffsetDateTime.now());
        evento.setResolvidoPor(body.getOrDefault("resolvidoPor", "admin"));
        return ResponseEntity.ok(eventoNaoCategorizadoRepository.save(evento));
    }

    // Regista auditoria
    @PostMapping("/audit")
    public ResponseEntity<CategorizationAudit> logAudit(@RequestBody CategorizationAudit audit) {
        return ResponseEntity.ok(controlador.logAudit(audit));
    }

    // Reprocessa categorização
    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, String>> reprocess() {
        controlador.reprocess();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Reprocessamento concluído."));
    }

    // Reset completo
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> reset() {
        controlador.reset();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Sistema limpo com sucesso."));
    }

    // Apaga eventos não categorizados
    @DeleteMapping("/uncategorized")
    public ResponseEntity<Void> deleteUncategorized() {
        controlador.deleteUncategorized();
        return ResponseEntity.noContent().build();
    }
}
