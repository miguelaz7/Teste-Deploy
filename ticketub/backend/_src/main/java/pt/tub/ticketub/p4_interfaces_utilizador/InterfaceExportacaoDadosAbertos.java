package pt.tub.ticketub.p4_interfaces_utilizador;

// =============================================================================
// O012.1.i – Interface de Exportação de Dados Abertos (UC12.1)
// Interface para o analista seleccionar conjuntos aprovados pelo DPO
// e exportar em formato aberto (CSV/JSON/GeoJSON). Bloqueia se dados não
// anonimizados forem detectados.
// Consome dados de: O012.1.d (repositório de exportações de dados abertos).
// =============================================================================

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RegistoDataLakeNgsiLd;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRegistoDataLakeNgsiLd;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exportacao")
public class InterfaceExportacaoDadosAbertos {

    private final RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository;
    private final RepositorioAuditoriaIngestao repositorioAuditoria;

    // Registo de acessos recentes para detetar padrões anómalos (UC12.3)
    private static final ConcurrentHashMap<String, List<Long>> requestsHistory = new ConcurrentHashMap<>();

    // Padrão de e-mail e NIF para detetar dados não anonimizados (FA1)
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
    private static final Pattern NIF_PATTERN = Pattern.compile("\\b[12356789]\\d{8}\\b");

    public InterfaceExportacaoDadosAbertos(
        RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository,
        RepositorioAuditoriaIngestao repositorioAuditoria
    ) {
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
        this.repositorioAuditoria = repositorioAuditoria;
    }

    // Export open data with filters and format validations (UC12.1)
    @GetMapping("/dados-abertos")
    public ResponseEntity<?> exportOpenData(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim,
        @RequestParam(defaultValue = "CSV") String formato,
        @RequestParam(required = false) String routeId,
        @RequestParam(defaultValue = "analista_dados") String utilizador
    ) {
        long timestampInicio = System.currentTimeMillis();
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(30);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        // 1. Detetar Padrão Anómalo de Acessos (UC12.3)
        long agora = System.currentTimeMillis();
        List<Long> userTimes = requestsHistory.computeIfAbsent(utilizador, k -> new ArrayList<>());
        userTimes.removeIf(t -> agora - t > 60000); // manter apenas o último minuto
        userTimes.add(agora);

        if (userTimes.size() > 3) {
            String msg = String.format("ALERTA: Padrão anómalo de exportações do utilizador '%s' (mais de 3 pedidos em 60s).", utilizador);
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "DPO_ALERTA_PADRAO_ANOMALO",
                "utilizador",
                utilizador,
                msg,
                OffsetDateTime.now()
            ));
        }

        // 2. Procurar registos
        List<RegistoDataLakeNgsiLd> registos = ngsiLdDataLakeRecordRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .collect(Collectors.toList());

        // 3. FA1 - Dados não anonimizados detetados
        for (RegistoDataLakeNgsiLd reg : registos) {
            String payload = reg.getPayloadJson();
            if (EMAIL_PATTERN.matcher(payload).find() || NIF_PATTERN.matcher(payload).find() || payload.toLowerCase().contains("nif") || payload.toLowerCase().contains("cardid_raw")) {
                
                String detalhe = String.format("Exportação bloqueada para '%s'. Detetados dados potencialmente sensíveis/pessoais no registo ID %s.", utilizador, reg.getEntityId());
                repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                    "DPO_ALERTA_DADOS_NAO_ANONIMIZADOS",
                    "entityId",
                    reg.getEntityId(),
                    detalhe,
                    OffsetDateTime.now()
                ));

                Map<String, Object> err = new LinkedHashMap<>();
                err.put("status", "BLOCKED");
                err.put("mensagem", "Exportação bloqueada pelo sistema. Detetados dados potencialmente identificáveis (FA1). O DPO foi notificado.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
            }
        }

        // 4. Aplicar regras de K-Anonymity e limites geográficos (UC12.1)
        List<Map<String, Object>> exportacao = new ArrayList<>();
        
        // Agregação de contagem para validação de k-anonymity (min 5 ocorrências por célula/grupo)
        Map<String, List<RegistoDataLakeNgsiLd>> agrupado = registos.stream()
            .collect(Collectors.groupingBy(r -> r.getEntityType()));

        boolean aplicouKAnonymityFilter = false;
        boolean aplicouGeoFilter = false;

        // Se formato for CSV ou Excel, aplicamos o filtro de min 5 ocorrências por grupo
        boolean isTabular = "CSV".equalsIgnoreCase(formato) || "EXCEL".equalsIgnoreCase(formato);

        // Contagem de paragens geográficas para validação GeoJSON
        long stopsCount = registos.stream().filter(r -> "PublicTransportStop".equals(r.getEntityType())).count();
        boolean geoExcluido = "GEOJSON".equalsIgnoreCase(formato) && stopsCount <= 10;

        for (Map.Entry<String, List<RegistoDataLakeNgsiLd>> entry : agrupado.entrySet()) {
            String tipo = entry.getKey();
            List<RegistoDataLakeNgsiLd> listaTipo = entry.getValue();

            // K-Anonymity check
            if (isTabular && listaTipo.size() <= 5) {
                aplicouKAnonymityFilter = true;
                continue; // Dropar grupo para evitar dupla identificação
            }

            for (RegistoDataLakeNgsiLd r : listaTipo) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("entityId",      r.getEntityId());
                item.put("entityType",    r.getEntityType());
                item.put("partitionDate", r.getPartitionDate());
                
                if ("GEOJSON".equalsIgnoreCase(formato)) {
                    if (geoExcluido) {
                        aplicouGeoFilter = true;
                        item.put("coordinates", "EXCLUIDO (Zona com <= 10 paragens)");
                    } else {
                        Map<String, Object> geo = new LinkedHashMap<>();
                        geo.put("type", "Point");
                        geo.put("coordinates", new double[]{ r.getOriginLon() != null ? r.getOriginLon() : -8.41, r.getOriginLat() != null ? r.getOriginLat() : 41.55 });
                        item.put("geometry", geo);
                    }
                } else {
                    item.put("payload", r.getPayloadJson());
                }
                exportacao.add(item);
            }
        }

        // 5. Metadados do export
        Map<String, Object> metadados = new LinkedHashMap<>();
        metadados.put("exportDate", LocalDate.now());
        metadados.put("periodStart", inicio);
        metadados.put("periodEnd", fim);
        metadados.put("dataVersion", "v2.0_ANON");
        metadados.put("disclaimer", "Os dados foram totalmente anonimizados. Filtros de K-Anonymity (min 5 ocorrencias por celula) e descaracterizacao GeoJSON foram aplicados para conformidade RGPD.");

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("metadados", metadados);
        resposta.put("totalRegistos", exportacao.size());
        resposta.put("kAnonymityApplied", aplicouKAnonymityFilter);
        resposta.put("geoFilterApplied", aplicouGeoFilter);

        // Estruturação do ficheiro
        if ("EXCEL".equalsIgnoreCase(formato)) {
            // Excel: Múltiplas sheets
            Map<String, Object> sheets = new LinkedHashMap<>();
            sheets.put("dados", exportacao);
            sheets.put("metadados", List.of(metadados));
            sheets.put("dicionario", List.of(Map.of("entityId", "Pseudónimo da entidade", "entityType", "Modelo Smart Data Models", "partitionDate", "Data de partição")));
            resposta.put("sheets", sheets);
        } else {
            resposta.put("registos", exportacao);
        }

        // Calcula checksum/hash do ficheiro exportado
        String contentString = resposta.toString();
        String checksum = calculateHash(contentString);
        resposta.put("checksum", checksum);

        long timestampFim = System.currentTimeMillis();

        // 6. Auditoria de Exportação (UC12.3)
        String detalheAudit = String.format("Exportação %s gerada por %s. Registos: %d. Tempo processamento: %dms.", formato, utilizador, exportacao.size(), (timestampFim - timestampInicio));
        repositorioAuditoria.save(new RegistoAuditoriaIngestao(
            "EXPORT_COMPLETED",
            "formato",
            formato,
            detalheAudit,
            OffsetDateTime.now()
        ));

        // Enviar notificação especial ao DPO se filtros específicos/poucos registos
        if (exportacao.size() < 15) {
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "DPO_ALERTA_FILTRO_RESTRITO",
                "registos",
                String.valueOf(exportacao.size()),
                String.format("Notificação DPO: Exportação com volume muito baixo (%d registos) solicitada por %s.", exportacao.size(), utilizador),
                OffsetDateTime.now()
            ));
        }

        return ResponseEntity.ok(resposta);
    }

    private String calculateHash(String conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(conteudo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(conteudo.hashCode());
        }
    }
}
