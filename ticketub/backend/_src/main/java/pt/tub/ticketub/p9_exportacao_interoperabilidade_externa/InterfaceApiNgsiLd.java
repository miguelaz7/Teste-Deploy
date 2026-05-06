package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

// =============================================================================
// O012.2.i – Interface API NGSI-LD (UC12.2)
// API RESTful documentada em OpenAPI, conforme NGSI-LD/Smart Data Models.
// Autenticação forte obrigatória. Suporta publicação e consulta de entidades.
// Consome dados de: O012.2.c (ControladorIntegracaoNgsiLd)
//                   O012.2.d (NgsiLdDataLakeRecordRepository)
//                   O012.1.d (ExportacaoDadosAbertosRepository)
// =============================================================================

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ngsi-ld")
public class InterfaceApiNgsiLd {

    private final ControladorIntegracaoNgsiLd controlador;
    private final NgsiLdDataLakeRecordRepository ngsiLdRepository;
    private final ExportacaoDadosAbertosRepository exportacaoRepository;

    public InterfaceApiNgsiLd(
        ControladorIntegracaoNgsiLd controlador,
        NgsiLdDataLakeRecordRepository ngsiLdRepository,
        ExportacaoDadosAbertosRepository exportacaoRepository
    ) {
        this.controlador        = controlador;
        this.ngsiLdRepository   = ngsiLdRepository;
        this.exportacaoRepository = exportacaoRepository;
    }

    // UC12.2 — Publicar entidade NGSI-LD de sistema externo
    @PostMapping("/entities")
    public ResponseEntity<Map<String, Object>> publicarEntidade(
        @RequestBody Map<String, Object> payload,
        @RequestHeader(value = "X-Api-User", defaultValue = "externo") String utilizador
    ) {
        NgsiLdDataLakeRecord record = controlador.publicarEntidade(payload, utilizador);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("entityId",   record.getEntityId());
        resposta.put("entityType", record.getEntityType());
        resposta.put("criadoEm",   record.getCreatedAt());
        resposta.put("estado",     "PUBLICADA");

        return ResponseEntity.ok(resposta);
    }

    // UC12.2 — Consultar entidades NGSI-LD por tipo
    @GetMapping("/entities")
    public ResponseEntity<List<Map<String, Object>>> consultarEntidades(
        @RequestParam(required = false) String type,
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<NgsiLdDataLakeRecord> registos = ngsiLdRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .filter(r -> type == null || type.equals(r.getEntityType()))
            .collect(Collectors.toList());

        List<Map<String, Object>> lista = registos.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("entityId",      r.getEntityId());
            m.put("entityType",    r.getEntityType());
            m.put("partitionDate", r.getPartitionDate());
            m.put("payload",       r.getPayloadJson());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    // UC12.1 — Solicitar exportação de dados abertos (aprovação DPO obrigatória)
    @PostMapping("/exportacoes")
    public ResponseEntity<Map<String, Object>> solicitarExportacao(
        @RequestBody Map<String, Object> params,
        @RequestHeader(value = "X-Api-User", defaultValue = "analista") String utilizador
    ) {
        String formato    = (String) params.getOrDefault("formato", "JSON");
        String filtros    = params.containsKey("filtros") ? params.get("filtros").toString() : null;
        LocalDate inicio  = LocalDate.parse(params.getOrDefault("periodoInicio",
            LocalDate.now().minusDays(7).toString()).toString());
        LocalDate fim     = LocalDate.parse(params.getOrDefault("periodoFim",
            LocalDate.now().toString()).toString());

        long totalRegistos = ngsiLdRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .count();

        ExportacaoDadosAbertos exportacao = new ExportacaoDadosAbertos(
            utilizador, formato, filtros, inicio, fim, totalRegistos, OffsetDateTime.now()
        );
        exportacaoRepository.save(exportacao);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id",             exportacao.getId());
        resposta.put("estado",         "PENDENTE_DPO");
        resposta.put("totalRegistos",  totalRegistos);
        resposta.put("mensagem",       "Exportacao submetida para aprovacao DPO.");

        return ResponseEntity.ok(resposta);
    }

    // UC12.1 — DPO aprova ou rejeita exportação
    @PutMapping("/exportacoes/{id}/aprovar")
    public ResponseEntity<Map<String, Object>> aprovarExportacao(
        @PathVariable Long id,
        @RequestBody Map<String, Object> body,
        @RequestHeader(value = "X-Api-User", defaultValue = "dpo") String dpo
    ) {
        ExportacaoDadosAbertos exportacao = exportacaoRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Exportacao nao encontrada: " + id));

        String decisao = (String) body.getOrDefault("decisao", "APROVADA");
        exportacao.setEstado(decisao);
        exportacao.setAprovadoPor(dpo);
        exportacao.setAprovadoEm(OffsetDateTime.now());

        if ("APROVADA".equals(decisao)) {
            String hash = controlador.calcularHash(exportacao.getId() + exportacao.getCriadoEm().toString());
            exportacao.setHashFicheiro(hash);
            exportacao.setEstado("EXPORTADA");
        }

        exportacaoRepository.save(exportacao);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("id",     exportacao.getId());
        resposta.put("estado", exportacao.getEstado());
        resposta.put("hash",   exportacao.getHashFicheiro());

        return ResponseEntity.ok(resposta);
    }

    // Lista todas as exportações
    @GetMapping("/exportacoes")
    public ResponseEntity<List<ExportacaoDadosAbertos>> listarExportacoes(
        @RequestParam(required = false) String estado
    ) {
        List<ExportacaoDadosAbertos> lista = estado != null
            ? exportacaoRepository.findByEstado(estado)
            : exportacaoRepository.findAll();
        return ResponseEntity.ok(lista);
    }
}
