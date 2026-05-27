package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

// =============================================================================
// O0.3.2.i – Interface de Administração RGPD
// Interface exclusiva do DPO para configurar regras de anonimização,
// períodos de retenção e aprovar/rejeitar conjuntos de dados para exportação.
// Consome dados de: O0.3.2.d (Repositório de Políticas de Anonimização).
// =============================================================================

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rgpd")
public class InterfaceAdministracaoRGPD {

    private final RepositorioPoliticaAnonimizacao politicaRepository;

    public InterfaceAdministracaoRGPD(RepositorioPoliticaAnonimizacao politicaRepository) {
        this.politicaRepository = politicaRepository;
    }

    // Lista todas as políticas de anonimização activas
    @GetMapping("/politicas")
    public ResponseEntity<List<PoliticaAnonimizacao>> listPolicies() {
        return ResponseEntity.ok(politicaRepository.findByStatus("ATIVA"));
    }

    // DPO cria uma nova política de anonimização
    @PostMapping("/politicas")
    public ResponseEntity<PoliticaAnonimizacao> createPolicy(@RequestBody PoliticaAnonimizacao politica) {
        politica.setStatus("ATIVA");
        politica.setApprovedAt(OffsetDateTime.now());
        return ResponseEntity.ok(politicaRepository.save(politica));
    }

    // DPO actualiza uma política existente
    @PutMapping("/politicas/{id}")
    public ResponseEntity<PoliticaAnonimizacao> updatePolicy(
        @PathVariable Long id, @RequestBody PoliticaAnonimizacao dados
    ) {
        PoliticaAnonimizacao existente = politicaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Política não encontrada: " + id));
        existente.setField(dados.getField());
        existente.setMethod(dados.getMethod());
        existente.setRetentionDays(dados.getRetentionDays());
        existente.setNotes(dados.getNotes());
        existente.setApprovedAt(OffsetDateTime.now());
        return ResponseEntity.ok(politicaRepository.save(existente));
    }

    // DPO revoga uma política (não apaga — fica com estado REVOGADA)
    @DeleteMapping("/politicas/{id}")
    public ResponseEntity<Map<String, String>> revokePolicy(@PathVariable Long id) {
        PoliticaAnonimizacao existente = politicaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Política não encontrada: " + id));
        existente.setStatus("REVOGADA");
        politicaRepository.save(existente);
        return ResponseEntity.ok(Map.of("status", "REVOGADA", "campo", existente.getField()));
    }
}
