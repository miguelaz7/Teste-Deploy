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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRegistoDataLakeNgsiLd;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rgpd")
public class InterfaceAdministracaoRGPD {

    private final RepositorioPoliticaAnonimizacao politicaRepository;
    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioRegistoDataLakeNgsiLd dataLakeRepository;
    private final RepositorioQuarentenaValidacao quarantineRepository;
    private final RepositorioAuditoriaIngestao auditRepository;

    @Value("${app.ingestion.pseudonym.secret:dev-secret-change-me}")
    private String pseudonymSecret;

    public InterfaceAdministracaoRGPD(
        RepositorioPoliticaAnonimizacao politicaRepository,
        RepositorioEventoValidacao validationEventRepository,
        RepositorioRegistoDataLakeNgsiLd dataLakeRepository,
        RepositorioQuarentenaValidacao quarantineRepository,
        RepositorioAuditoriaIngestao auditRepository
    ) {
        this.politicaRepository = politicaRepository;
        this.validationEventRepository = validationEventRepository;
        this.dataLakeRepository = dataLakeRepository;
        this.quarantineRepository = quarantineRepository;
        this.auditRepository = auditRepository;
    }

    // Lista todas as políticas de anonimização activas
    @GetMapping("/politicas")
    public ResponseEntity<List<PoliticaAnonimizacao>> listPolicies() {
        return ResponseEntity.ok(politicaRepository.findByStatus("ATIVA"));
    }

    // DPO cria uma nova política de anonimização
    @PostMapping("/politicas")
    @Transactional
    public ResponseEntity<PoliticaAnonimizacao> createPolicy(@RequestBody PoliticaAnonimizacao politica) {
        politica.setStatus("ATIVA");
        politica.setApprovedAt(OffsetDateTime.now());
        PoliticaAnonimizacao saved = politicaRepository.save(politica);

        auditRepository.save(new RegistoAuditoriaIngestao(
            "RGPD_POLICY_CREATE", "field", saved.getField(),
            "Nova politica de anonimizacao aprovada por: " + saved.getApprovedBy() + " com metodo: " + saved.getMethod(),
            OffsetDateTime.now()
        ));

        return ResponseEntity.ok(saved);
    }

    // DPO actualiza uma política existente
    @PutMapping("/politicas/{id}")
    @Transactional
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
        if (dados.getApprovedBy() != null) {
            existente.setApprovedBy(dados.getApprovedBy());
        }
        PoliticaAnonimizacao saved = politicaRepository.save(existente);

        auditRepository.save(new RegistoAuditoriaIngestao(
            "RGPD_POLICY_UPDATE", "field", saved.getField(),
            "Politica de anonimizacao atualizada por: " + saved.getApprovedBy() + " com metodo: " + saved.getMethod(),
            OffsetDateTime.now()
        ));

        return ResponseEntity.ok(saved);
    }

    // DPO revoga uma política (não apaga — fica com estado REVOGADA)
    @DeleteMapping("/politicas/{id}")
    @Transactional
    public ResponseEntity<Map<String, String>> revokePolicy(@PathVariable Long id) {
        PoliticaAnonimizacao existente = politicaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Política não encontrada: " + id));
        existente.setStatus("REVOGADA");
        politicaRepository.save(existente);

        auditRepository.save(new RegistoAuditoriaIngestao(
            "RGPD_POLICY_REVOKE", "field", existente.getField(),
            "Politica de anonimizacao revogada",
            OffsetDateTime.now()
        ));

        return ResponseEntity.ok(Map.of("status", "REVOGADA", "campo", existente.getField()));
    }

    // Direito ao Esquecimento: Eliminação completa dos dados de um cartão
    @PostMapping("/direito-ao-esquecimento")
    @Transactional
    public ResponseEntity<Map<String, Object>> rightToBeForgotten(
        @RequestBody Map<String, String> body,
        @RequestHeader(value = "X-Api-User", defaultValue = "dpo") String dpo
    ) {
        String cardId = body.get("cardId");
        if (cardId == null || cardId.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "cardId obrigatorio"));
        }
        cardId = cardId.trim();

        // Calcular hash de pseudonimização
        String hashedCardId = pseudonymize(cardId);

        // Delete matches in validation events (using hashed value)
        validationEventRepository.deleteByCardId(hashedCardId);

        // Delete matches in data lake (using pattern match for payload)
        dataLakeRepository.deleteByCardIdPattern("%" + hashedCardId + "%");

        // Delete matches in quarantine logs (using raw cardId value)
        quarantineRepository.deleteByCardIdPattern("%" + cardId + "%");

        // Auditar a acção de Direito ao Esquecimento
        auditRepository.save(new RegistoAuditoriaIngestao(
            "RIGHT_TO_BE_FORGOTTEN", "cardId", "PSEUDONYMIZED_HASH",
            "Direito ao esquecimento executado pelo DPO: " + dpo + " para o cartao hashed: " + hashedCardId,
            OffsetDateTime.now()
        ));

        return ResponseEntity.ok(Map.of(
            "status", "SUCCESS",
            "message", "Dados do cartao eliminados de forma definitiva.",
            "cardIdHashed", hashedCardId
        ));
    }

    private String pseudonymize(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(pseudonymSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = hmac.doFinal(valor.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(valor.hashCode());
        }
    }
}
