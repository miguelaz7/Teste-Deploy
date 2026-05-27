package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p5_analise_operacional_tempo_real.ControladorAgregacaoProcura;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Paragem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioParagem;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// =============================================================================
// O0.6.1.i – Interface do Mapa de Rede (UC06.1, UC06.2, UC06.3)
// Mapa interactivo com marcadores das paragens. Ao clicar, abre popup
// LIVE DATA com afluência, validações inválidas e distribuição por perfil
// tarifário. Consome dados de: O0.5.1.c, O0.5.1.d e quarentena (P7).
// =============================================================================

@RestController
@RequestMapping("/api/mapa")
public class InterfaceMapaRede {

    private final RepositorioParagem stopRepository;
    private final ControladorAgregacaoProcura controladorAgregacaoProcura;
    private final RepositorioQuarentenaValidacao validationQuarantineRepository;
    private final RepositorioEventoValidacao validationEventRepository;

    private static final Set<String> VALID_RESULTS = Set.of(
        "VALID", "OK", "SUCCESS", "ACEITE", "ACEITO", "APROVADO", "ACCEPTED", "1"
    );

    public InterfaceMapaRede(RepositorioParagem stopRepository,
                             ControladorAgregacaoProcura controladorAgregacaoProcura,
                             RepositorioQuarentenaValidacao validationQuarantineRepository,
                             RepositorioEventoValidacao validationEventRepository) {
        this.stopRepository                 = stopRepository;
        this.controladorAgregacaoProcura    = controladorAgregacaoProcura;
        this.validationQuarantineRepository = validationQuarantineRepository;
        this.validationEventRepository      = validationEventRepository;
    }

    // Lista todas as paragens com coordenadas para marcadores no mapa
    @GetMapping("/paragens")
    public ResponseEntity<List<Map<String, Object>>> getStops() {
        List<Map<String, Object>> paragens = stopRepository.findAll().stream()
            .map(this::toMarker)
            .collect(Collectors.toList());
        return ResponseEntity.ok(paragens);
    }

    // UC06.1 — Recuperar ID da paragem a partir das coordenadas com tolerância de 50m
    @GetMapping("/paragens/procura-por-coordenadas")
    public ResponseEntity<Map<String, Object>> getStopByCoordinates(@RequestParam double lat, @RequestParam double lon) {
        List<Paragem> paragens = stopRepository.findAll();
        Paragem maisProxima = null;
        double menorDistancia = Double.MAX_VALUE;

        for (Paragem p : paragens) {
            double dist = calcularDistancia(lat, lon, p.getStopLat(), p.getStopLon());
            if (dist < menorDistancia) {
                menorDistancia = dist;
                maisProxima = p;
            }
        }

        if (maisProxima == null || menorDistancia > 50.0) {
            return ResponseEntity.notFound().build();
        }

        return getLiveData(maisProxima.getStopId());
    }

    // UC06.1, UC06.2, UC06.3 — Popup LIVE DATA completo
    @GetMapping("/paragens/{stopId}/live-data")
    public ResponseEntity<Map<String, Object>> getLiveData(@PathVariable String stopId) {
        Optional<Paragem> stopOpt = stopRepository.findById(stopId);
        if (stopOpt.isEmpty()) return ResponseEntity.notFound().build();

        List<EventoValidacao> todosEventos = validationEventRepository.findByOriginStop_StopId(stopId);

        // UC06.1 — consulta últimos 5 minutos de eventos válidos
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime a5MinutosAtras = agora.minusMinutes(5);

        List<EventoValidacao> eventosUltimos5MinutosValidos = todosEventos.stream()
            .filter(e -> e.getTransactionDateTime() != null && !e.getTransactionDateTime().isBefore(a5MinutosAtras))
            .filter(e -> !isInvalid(e))
            .collect(Collectors.toList());

        long totalValidasUltimos5Minutos = eventosUltimos5MinutosValidos.size();

        // Distribuição por perfil tarifário nos últimos 5 minutos
        Map<String, Long> ticketTypeDistribution5Min = new LinkedHashMap<>();
        for (EventoValidacao e : eventosUltimos5MinutosValidos) {
            ticketTypeDistribution5Min.merge(normalizeType(e), 1L, Long::sum);
        }

        // Última atualização (timestamp)
        OffsetDateTime ultimaAtualizacao = eventosUltimos5MinutosValidos.stream()
            .map(EventoValidacao::getTransactionDateTime)
            .max(Comparator.naturalOrder())
            .orElse(agora);

        // UC06.1 — Compara com baseline (mesma paragem, mesma hora, últimas 4 semanas)
        OffsetDateTime quatroSemanasAtras = agora.minusWeeks(4);
        int targetHour = agora.getHour();

        List<EventoValidacao> historicoMesmaHora = todosEventos.stream()
            .filter(e -> e.getTransactionDateTime() != null 
                    && e.getTransactionDateTime().isAfter(quatroSemanasAtras)
                    && e.getTransactionDateTime().isBefore(agora))
            .filter(e -> e.getTransactionDateTime().getHour() == targetHour)
            .filter(e -> !isInvalid(e))
            .collect(Collectors.toList());

        double baselineHoraMedia = historicoMesmaHora.size() / 4.0;
        double baseline5MinMedia = baselineHoraMedia / 12.0;

        if (baseline5MinMedia < 0.5) {
            baseline5MinMedia = 1.0; // Evitar divisões ou baselines vazias
        }

        // UC06.1 & UC06.2 — Identifica se procura está acima/abaixo da média
        String afluenciaStatus = "NA_MEDIA";
        if (totalValidasUltimos5Minutos > baseline5MinMedia * 1.2) {
            afluenciaStatus = "ACIMA_DA_MEDIA";
        } else if (totalValidasUltimos5Minutos < baseline5MinMedia * 0.8) {
            afluenciaStatus = "ABAIXO_DA_MEDIA";
        }

        // UC06.2 — Botão "Ver histórico" abre gráfico de procura da paragem para últimas 24h
        Map<String, Long> historico24h = new LinkedHashMap<>();
        OffsetDateTime vinteQuatroHorasAtras = agora.minusHours(24);
        for (int i = 23; i >= 0; i--) {
            OffsetDateTime h = agora.minusHours(i);
            String label = String.format("%02d:00", h.getHour());
            historico24h.put(label, 0L);
        }

        todosEventos.stream()
            .filter(e -> e.getTransactionDateTime() != null 
                    && e.getTransactionDateTime().isAfter(vinteQuatroHorasAtras)
                    && e.getTransactionDateTime().isBefore(agora))
            .filter(e -> !isInvalid(e))
            .forEach(e -> {
                String label = String.format("%02d:00", e.getTransactionDateTime().getHour());
                historico24h.merge(label, 1L, Long::sum);
            });

        // UC06.3 — inválidas por paragem (quarentena P7)
        long totalInvalidas  = validationQuarantineRepository.countByOriginStopId(stopId);
        long totalValidasTotal = todosEventos.stream().filter(e -> !isInvalid(e)).count();
        long totalGeral      = totalValidasTotal + totalInvalidas;
        double taxaInvalidas = totalGeral > 0
            ? Math.round((double) totalInvalidas / totalGeral * 10000.0) / 100.0 : 0.0;

        // UC06.3 — top 3 motivos de rejeição
        List<Object[]> topMotivos = validationQuarantineRepository
            .findTopReasonsByOriginStopId(stopId);
        List<Map<String, Object>> top3 = topMotivos.stream().limit(3).map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("motivo", row[0].toString());
            m.put("total",  ((Number) row[1]).longValue());
            return m;
        }).collect(Collectors.toList());

        // UC06.3 — alerta qualidade degradada se taxa > 2%
        boolean qualidadeDegradada = taxaInvalidas > 2.0;

        // Janela de pico (historica)
        Map<String, Object> insights = controladorAgregacaoProcura.getInsights(Optional.of(stopId));
        String peakWindowStart = "";
        String peakWindowEnd   = "";
        double peakPercentage  = 0.0;
        if (insights.containsKey("timeGap") && insights.get("timeGap") != null) {
            String timeGap = insights.get("timeGap").toString();
            peakWindowStart = timeGap.length() >= 5 ? timeGap.substring(0, 5) : timeGap;
            try {
                int startHour = Integer.parseInt(peakWindowStart.substring(0, 2));
                peakWindowEnd = String.format("%02d:00", (startHour + 1) % 24);
            } catch (Exception e) {
                peakWindowEnd = peakWindowStart;
            }
        }
        if (insights.containsKey("peakAfluenciaPercentage")) {
            peakPercentage = ((Number) insights.get("peakAfluenciaPercentage")).doubleValue();
        }

        // Resposta completa
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("stopId",                                   stopOpt.get().getStopId());
        resposta.put("stopName",                                 stopOpt.get().getStopName());
        resposta.put("peakHourWindowStart",                      peakWindowStart);
        resposta.put("peakHourWindowEnd",                        peakWindowEnd);
        resposta.put("peakHourValidationsCount",                 totalValidasTotal > 0 ? (long)(totalValidasTotal * peakPercentage / 100) : 0L);
        resposta.put("peakHourValidationsPercentageOfStopTotal", peakPercentage);
        resposta.put("invalidValidationsCount",                  totalInvalidas);
        resposta.put("invalidValidationsPercentageOfStopTotal",  taxaInvalidas);
        resposta.put("qualidadeDegradada",                       qualidadeDegradada);
        resposta.put("topMotivoRejeicao",                        top3);
        resposta.put("ticketTypeDistribution",                   ticketTypeDistribution5Min); // 5 min distribution for popup
        resposta.put("totalValidations",                         totalValidasUltimos5Minutos); // 5 min total for popup
        resposta.put("afluenciaStatus",                          afluenciaStatus);
        resposta.put("historico24h",                             historico24h);
        resposta.put("lastUpdated",                              ultimaAtualizacao.format(DateTimeFormatter.ofPattern("HH:mm:ss")));

        return ResponseEntity.ok(resposta);
    }

    private Map<String, Object> toMarker(Paragem stop) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stopId",   stop.getStopId());
        m.put("stopName", stop.getStopName());
        m.put("lat",      stop.getStopLat());
        m.put("lon",      stop.getStopLon());
        return m;
    }

    private double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio da Terra em metros
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private boolean isInvalid(EventoValidacao e) {
        if (e.getRejectReason() != null && !e.getRejectReason().isBlank()) return true;
        String result = e.getResult();
        if (result == null || result.isBlank()) return true;
        return !VALID_RESULTS.contains(result.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeType(EventoValidacao e) {
        if (e.getTicketType() == null || e.getTicketType().getCode() == null) return "AVULSO";
        String code = e.getTicketType().getCode().toUpperCase(Locale.ROOT);
        if (code.contains("ESTUDANTE")) return "ESTUDANTE";
        if (code.contains("SOCIAL"))    return "PASSE_SOCIAL";
        if (code.contains("SENIOR"))    return "SENIOR";
        if (code.contains("MENSAL") || code.contains("MONTHLY")) return "MENSAL";
        return "AVULSO";
    }
}