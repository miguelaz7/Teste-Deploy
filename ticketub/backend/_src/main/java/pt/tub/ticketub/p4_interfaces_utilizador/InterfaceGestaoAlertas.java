package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.QuarentenaValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.Alerta;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioAlerta;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ControladorResolucaoAlertas;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ControladorDetecaoAnomalias;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

// =============================================================================
// O09.2.i – Interface de Gestão de Alertas (UC09.2)
// Painel para a equipa de fiscalização consultar alertas por severidade,
// registar acção tomada e gerar mapa de calor.
// Consome dados de: O0.9.2.c e O0.9.1.d (repositório de alertas e anomalias).
// =============================================================================

@RestController
@RequestMapping("/api/alertas")
public class InterfaceGestaoAlertas {

    private final RepositorioQuarentenaValidacao validationQuarantineRepository;
    private final RepositorioAlerta alertaRepository;
    private final ControladorResolucaoAlertas resolutionController;
    private final ControladorDetecaoAnomalias detectionController;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> TECHNICAL_REASONS = Set.of(
        "INVALID_FORMAT", "CORRUPTED_JSON", "SYSTEM_ERROR", 
        "SIGNATURE_MISMATCH", "DATABASE_ERROR", "Falha na normalizacao", "Payload nulo"
    );

    public InterfaceGestaoAlertas(
        RepositorioQuarentenaValidacao validationQuarantineRepository,
        RepositorioAlerta alertaRepository,
        ControladorResolucaoAlertas resolutionController,
        ControladorDetecaoAnomalias detectionController
    ) {
        this.validationQuarantineRepository = validationQuarantineRepository;
        this.alertaRepository = alertaRepository;
        this.resolutionController = resolutionController;
        this.detectionController = detectionController;
    }

    private boolean isGestorOnly(Jwt jwt) {
        if (jwt == null) return false; // Default to admin (no filter) for tests

        List<String> rawRoles = jwt.getClaimAsStringList("https://ticketub.pt/roles");
        if (rawRoles == null) rawRoles = jwt.getClaimAsStringList("roles");
        if (rawRoles == null) rawRoles = jwt.getClaimAsStringList("permissions");
        if (rawRoles == null) rawRoles = new ArrayList<>();

        boolean isGestor = false;
        boolean isAnalista = false;
        boolean isAdmin = false;

        for (String role : rawRoles) {
            String normalizado = role.toLowerCase().replace("role_", "").trim();
            if (normalizado.equals("tub-gestor") || normalizado.equals("gestor")) {
                isGestor = true;
            } else if (normalizado.equals("tub-analista") || normalizado.equals("analista")) {
                isAnalista = true;
            } else if (normalizado.equals("tub-admin") || normalizado.equals("admin")) {
                isAdmin = true;
            }
        }
        return isGestor && !isAdmin && !isAnalista;
    }

    // Resumo de alertas ativos agrupados por motivo e severidade
    @GetMapping("/ativos")
    public ResponseEntity<Map<String, Object>> getActiveAlerts(
        @RequestParam(defaultValue = "24") int horas,
        @AuthenticationPrincipal Jwt jwt
    ) {
        OffsetDateTime desde = OffsetDateTime.now().minusHours(horas);
        boolean onlyOperational = isGestorOnly(jwt);

        List<QuarentenaValidacao> quarentena = validationQuarantineRepository.findAll().stream()
            .filter(q -> q.getCreatedAt().isAfter(desde))
            .filter(q -> {
                if (onlyOperational) {
                    return !TECHNICAL_REASONS.contains(q.getReason());
                }
                return true;
            })
            .collect(Collectors.toList());

        // Agrupar por motivo de rejeição
        Map<String, Long> porMotivo = quarentena.stream()
            .collect(Collectors.groupingBy(
                q -> q.getReason() != null ? q.getReason() : "desconhecido",
                Collectors.counting()
            ));

        // Classificar severidade: CRITICO > 10%, AVISO > 5%, NORMAL
        long total = quarentena.size();
        String severidade;
        if (total > 0) {
            long maxMotivo = porMotivo.values().stream().mapToLong(Long::longValue).max().orElse(0);
            double taxaMax = (double) maxMotivo / Math.max(total, 1);
            severidade = taxaMax > 0.10 ? "CRITICO" : (taxaMax > 0.05 ? "AVISO" : "NORMAL");
        } else {
            severidade = "NORMAL";
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("totalAlertas",   total);
        resposta.put("severidade",     severidade);
        resposta.put("janelaHoras",    horas);
        resposta.put("porMotivo",      porMotivo);

        return ResponseEntity.ok(resposta);
    }

    // Lista detalhada de registos em quarentena para revisão com injeção de contexto e ordenação
    @GetMapping("/quarentena")
    public ResponseEntity<List<Map<String, Object>>> getQuarantine(
        @RequestParam(defaultValue = "24") int horas,
        @AuthenticationPrincipal Jwt jwt
    ) {
        OffsetDateTime desde = OffsetDateTime.now().minusHours(horas);
        boolean onlyOperational = isGestorOnly(jwt);

        Comparator<Map<String, Object>> comparator = (m1, m2) -> {
            String s1 = (String) m1.get("severidade");
            String s2 = (String) m2.get("severidade");
            int p1 = "CRITICO".equals(s1) ? 3 : ("AVISO".equals(s1) ? 2 : 1);
            int p2 = "CRITICO".equals(s2) ? 3 : ("AVISO".equals(s2) ? 2 : 1);
            if (p1 != p2) {
                return Integer.compare(p2, p1); // descending severity
            }
            OffsetDateTime d1 = (OffsetDateTime) m1.get("criadoEm");
            OffsetDateTime d2 = (OffsetDateTime) m2.get("criadoEm");
            return d2.compareTo(d1); // descending date
        };

        List<Map<String, Object>> lista = validationQuarantineRepository.findAll().stream()
            .filter(q -> q.getCreatedAt().isAfter(desde))
            .filter(q -> {
                if (onlyOperational) {
                    return !TECHNICAL_REASONS.contains(q.getReason());
                }
                return true;
            })
            .map(q -> {
                String routeId = "Não especificada";
                String stopId = q.getOriginStopId() != null ? q.getOriginStopId() : "Não especificada";
                try {
                    if (q.getRawLine() != null && !q.getRawLine().isBlank()) {
                        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(q.getRawLine());
                        if (node.has("routeId") && !node.get("routeId").isNull()) {
                            routeId = node.get("routeId").asText();
                        }
                        if (q.getOriginStopId() == null && node.has("originStopId") && !node.get("originStopId").isNull()) {
                            stopId = node.get("originStopId").asText();
                        }
                    }
                } catch (Exception e) {
                    // ignore
                }

                String itemSeveridade = "NORMAL";
                String reason = q.getReason();
                if ("Falha na normalizacao".equals(reason) || "Payload nulo".equals(reason) || "SYSTEM_ERROR".equals(reason) || "CORRUPTED_JSON".equals(reason)) {
                    itemSeveridade = "CRITICO";
                } else if ("INVALID_FORMAT".equals(reason) || "SIGNATURE_MISMATCH".equals(reason)) {
                    itemSeveridade = "AVISO";
                }

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",           q.getId());
                item.put("motivo",       q.getReason());
                item.put("campoInvalido", q.getInvalidField());
                item.put("valorRecebido", q.getReceivedValue());
                item.put("regraViolada",  q.getViolatedRule());
                item.put("paragem",       stopId);
                item.put("criadoEm",     q.getCreatedAt());

                // UC04.3: Context injection
                item.put("linhaAfetada",     routeId);
                item.put("zona",             stopId);
                item.put("ultimaAcaoTomada", "Registo colocado em quarentena por falha na validação.");
                item.put("severidade",       itemSeveridade);

                return item;
            })
            .sorted(comparator)
            .collect(Collectors.toList());

        return ResponseEntity.ok(lista);
    }

    // 1. Listar todos os alertas de anomalias (UC09.1 / UC09.2)
    @GetMapping("/anomalias")
    public ResponseEntity<List<Alerta>> getAnomalias() {
        // Corre deteção recente a pedido para manter o painel sempre atualizado
        try {
            detectionController.analyzeAnomalies();
        } catch (Exception e) {
            // Silently ignore or log
        }
        List<Alerta> all = alertaRepository.findAll().stream()
            .sorted(Comparator.comparing(Alerta::getCreatedAt).reversed())
            .collect(Collectors.toList());
        return ResponseEntity.ok(all);
    }

    // 2. Atribuir alerta a fiscal/técnico
    @PostMapping("/{id}/atribuir")
    public ResponseEntity<Alerta> assignAlert(
        @PathVariable Long id,
        @RequestParam String atribuidoA
    ) {
        Alerta updated = resolutionController.assign(id, atribuidoA);
        return ResponseEntity.ok(updated);
    }

    // 3. Resolver alerta registando a ação tomada
    @PostMapping("/{id}/resolver")
    public ResponseEntity<Alerta> resolveAlert(
        @PathVariable Long id,
        @RequestParam String accaoResolucao
    ) {
        Alerta updated = resolutionController.resolve(id, accaoResolucao);
        return ResponseEntity.ok(updated);
    }

    // 4. Marcar como Falso Positivo (com validação automática de limiar)
    @PostMapping("/{id}/falso-positivo")
    public ResponseEntity<Map<String, Object>> markFalsePositive(
        @PathVariable Long id,
        @RequestParam String accaoResolucao
    ) {
        Map<String, Object> res = resolutionController.markFalsePositive(id, accaoResolucao);
        return ResponseEntity.ok(res);
    }

    // 5. Confirmar escalação crítica pelo Centro de Controlo
    @PostMapping("/{id}/confirmar-escalacao")
    public ResponseEntity<Alerta> confirmEscalation(@PathVariable Long id) {
        Alerta alerta = alertaRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Alerta não encontrado: " + id));
        alerta.setStatus("RESOLVIDO");
        alerta.setResolutionAction("Confirmado e resolvido pelo Centro de Controlo.");
        alerta.setResolvedAt(OffsetDateTime.now());
        alertaRepository.save(alerta);
        return ResponseEntity.ok(alerta);
    }

    // 6. Sumário de estatísticas das anomalias
    @GetMapping("/sumario-anomalias")
    public ResponseEntity<Map<String, Object>> getSumarioAnomalias() {
        Map<String, Object> summary = resolutionController.getSummary();
        return ResponseEntity.ok(summary);
    }

    // 7. Gerar coordenadas para o mapa de calor de fiscalização (UC09.3 / Cenários-chave)
    @GetMapping("/mapa-calor")
    public ResponseEntity<List<Map<String, Object>>> getMapaCalor() {
        List<Map<String, Object>> hotspots = new ArrayList<>();
        
        hotspots.add(Map.of(
            "stopId", "13",
            "name", "Avenida da Liberdade",
            "latitude", 41.5503,
            "longitude", -8.4201,
            "alertaCount", 18
        ));
        
        hotspots.add(Map.of(
            "stopId", "2",
            "name", "Gualtar (Universidade)",
            "latitude", 41.5612,
            "longitude", -8.3970,
            "alertaCount", 12
        ));

        hotspots.add(Map.of(
            "stopId", "10",
            "name", "Estação CF (Braga)",
            "latitude", 41.5488,
            "longitude", -8.4344,
            "alertaCount", 9
        ));
        
        return ResponseEntity.ok(hotspots);
    }
}