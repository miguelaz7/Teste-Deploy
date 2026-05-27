package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

// =============================================================================
// O012.2.i – Interface API NGSI-LD (UC12.2)
// API RESTful documentada em OpenAPI, conforme NGSI-LD/Smart Data Models.
// Autenticação forte obrigatória. Suporta publicação e consulta de entidades.
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ngsi-ld")
public class InterfaceApiNgsiLd {

    private final ControladorIntegracaoNgsiLd controlador;
    private final RepositorioRegistoDataLakeNgsiLd ngsiLdRepository;
    private final RepositorioExportacaoDadosAbertos openDataExportRepository;
    private final RepositorioAuditoriaIngestao repositorioAuditoria;

    // Token fixo simulado para validação de autenticação forte da API (FA2)
    private static final String API_AUTH_TOKEN = "Bearer token_valido_ngsi_ld";

    public InterfaceApiNgsiLd(
        ControladorIntegracaoNgsiLd controlador,
        RepositorioRegistoDataLakeNgsiLd ngsiLdRepository,
        RepositorioExportacaoDadosAbertos openDataExportRepository,
        RepositorioAuditoriaIngestao repositorioAuditoria
    ) {
        this.controlador             = controlador;
        this.ngsiLdRepository        = ngsiLdRepository;
        this.openDataExportRepository = openDataExportRepository;
        this.repositorioAuditoria       = repositorioAuditoria;
    }

    // UC12.2 — Publicar entidade NGSI-LD de sistema externo com autenticação forte
    @PostMapping("/entities")
    public ResponseEntity<?> publishEntity(
        @RequestBody Map<String, Object> payload,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestHeader(value = "X-Api-User", defaultValue = "externo") String user
    ) {
        long tStart = System.currentTimeMillis();

        // 1. Validar autenticação (FA2)
        if (authHeader == null || !API_AUTH_TOKEN.equals(authHeader)) {
            String detalhe = String.format("Acesso negado à API: Token em falta ou inválido para o utilizador '%s'.", user);
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "API_AUTH_FAILED",
                "Authorization",
                authHeader != null ? "FORNECIDO" : "NULO",
                detalhe,
                OffsetDateTime.now()
            ));

            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Unauthorized");
            err.put("message", "Credenciais de API inválidas (FA2).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        // 2. Validar esquema e dados (FA3)
        try {
            RegistoDataLakeNgsiLd record = controlador.publishEntity(payload, user);

            Map<String, Object> resposta = new LinkedHashMap<>();
            resposta.put("entityId",   record.getEntityId());
            resposta.put("entityType", record.getEntityType());
            resposta.put("criadoEm",   record.getCreatedAt());
            resposta.put("estado",     "PUBLICADA");

            // Registar em auditoria (UC12.3)
            String det = String.format("Entidade %s publicada via API por %s.", record.getEntityId(), user);
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "API_CALL",
                "entityId",
                record.getEntityId(),
                det,
                OffsetDateTime.now()
            ));

            return ResponseEntity.ok(resposta);
        } catch (IllegalArgumentException ex) {
            // FA3 – Dados fora do esquema esperado
            String detalhe = String.format("Publicação rejeitada por inconformidade com Smart Data Models: %s.", ex.getMessage());
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "API_BAD_REQUEST",
                "payload",
                "INVALID_SCHEMA",
                detalhe,
                OffsetDateTime.now()
            ));

            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Bad Request");
            err.put("message", "Dados fora do esquema esperado (FA3).");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    // UC12.2 — Consultar entidades NGSI-LD por tipo com autenticação forte
    @GetMapping("/entities")
    public ResponseEntity<?> getEntities(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim,
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestHeader(value = "X-Api-User", defaultValue = "externo") String user
    ) {
        // 1. Validar autenticação (FA2)
        if (authHeader == null || !API_AUTH_TOKEN.equals(authHeader)) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("error", "Unauthorized");
            err.put("message", "Credenciais de API inválidas (FA2).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }

        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<RegistoDataLakeNgsiLd> registos = ngsiLdRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .filter(r -> type == null || type.equals(r.getEntityType()))
            .collect(Collectors.toList());

        List<Map<String, Object>> lista = registos.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",            r.getEntityId());
            m.put("type",          r.getEntityType());
            m.put("partitionDate", r.getPartitionDate());
            m.put("payload",       r.getPayloadJson());
            m.put("@context",      List.of("https://uri.etsi.org/ngsi-ld/v1/ngsi-ld-core-context.jsonld"));
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    // UC12.1 — Solicitar exportação de dados abertos (aprovação DPO obrigatória)
    @PostMapping("/exportacoes")
    public ResponseEntity<Map<String, Object>> requestExport(
        @RequestBody Map<String, Object> params,
        @RequestHeader(value = "X-Api-User", defaultValue = "analista") String user
    ) {
        String format     = (String) params.getOrDefault("formato", "JSON");
        String filters    = params.containsKey("filtros") ? params.get("filtros").toString() : null;
        LocalDate inicio  = LocalDate.parse(params.getOrDefault("periodoInicio",
            LocalDate.now().minusDays(7).toString()).toString());
        LocalDate fim     = LocalDate.parse(params.getOrDefault("periodoFim",
            LocalDate.now().toString()).toString());

        long totalRecords = ngsiLdRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .count();

        ExportacaoDadosAbertos exportacao = new ExportacaoDadosAbertos(
            user, format, filters, inicio, fim, totalRecords, OffsetDateTime.now()
        );
        openDataExportRepository.save(exportacao);

        // Registar pedido em auditoria (UC12.3)
        repositorioAuditoria.save(new RegistoAuditoriaIngestao(
            "EXPORT_REQUESTED",
            "formato",
            format,
            String.format("Pedido de exportacao %s submetido pelo utilizador %s.", format, user),
            OffsetDateTime.now()
        ));

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id",             exportacao.getId());
        resposta.put("estado",         "PENDENTE_DPO");
        resposta.put("totalRegistos",  totalRecords);
        resposta.put("mensagem",       "Exportacao submetida para aprovacao DPO.");

        return ResponseEntity.ok(resposta);
    }

    // UC12.1 — DPO aprova ou rejeita exportação
    @PutMapping("/exportacoes/{id}/aprovar")
    public ResponseEntity<Map<String, Object>> approveExport(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body,
        @RequestHeader(value = "X-Api-User", defaultValue = "dpo") String dpo
    ) {
        ExportacaoDadosAbertos exportacao = openDataExportRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exportacao nao encontrada: " + id));

        String decisao = (String) body.getOrDefault("decisao", "APROVADA");
        exportacao.setStatus(decisao);
        exportacao.setApprovedBy(dpo);
        exportacao.setApprovedAt(OffsetDateTime.now());

        if ("APROVADA".equals(decisao)) {
            String hash = controlador.calculateHash(exportacao.getId() + exportacao.getCreatedAt().toString());
            exportacao.setFileHash(hash);
            exportacao.setStatus("EXPORTADA");

            // Registar em auditoria (UC12.3)
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "EXPORT_APPROVED",
                "id",
                String.valueOf(id),
                String.format("Exportação %d aprovada pelo DPO %s. Hash gerada: %s", id, dpo, hash),
                OffsetDateTime.now()
            ));
        } else {
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "EXPORT_REJECTED",
                "id",
                String.valueOf(id),
                String.format("Exportação %d rejeitada pelo DPO %s.", id, dpo),
                OffsetDateTime.now()
            ));
        }

        openDataExportRepository.save(exportacao);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id",     exportacao.getId());
        resposta.put("estado", exportacao.getStatus());
        resposta.put("hash",   exportacao.getFileHash());

        return ResponseEntity.ok(resposta);
    }

    // Lista todas as exportações
    @GetMapping("/exportacoes")
    public ResponseEntity<List<ExportacaoDadosAbertos>> listExports(
        @RequestParam(required = false) String status
    ) {
        List<ExportacaoDadosAbertos> lista = status != null
            ? openDataExportRepository.findByStatus(status)
            : openDataExportRepository.findAll();
        return ResponseEntity.ok(lista);
    }
}
