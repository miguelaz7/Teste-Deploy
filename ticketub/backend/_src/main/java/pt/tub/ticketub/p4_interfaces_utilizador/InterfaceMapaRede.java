package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p5_analise_operacional_tempo_real.ControladorAgregacaoProcura;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantineRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Stop;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

    private final StopRepository stopRepository;
    private final ControladorAgregacaoProcura controladorAgregacaoProcura;
    private final ValidationQuarantineRepository validationQuarantineRepository;

    public InterfaceMapaRede(StopRepository stopRepository,
                             ControladorAgregacaoProcura controladorAgregacaoProcura,
                             ValidationQuarantineRepository validationQuarantineRepository) {
        this.stopRepository                 = stopRepository;
        this.controladorAgregacaoProcura    = controladorAgregacaoProcura;
        this.validationQuarantineRepository = validationQuarantineRepository;
    }

    // Lista todas as paragens com coordenadas para marcadores no mapa
    @GetMapping("/paragens")
    public ResponseEntity<List<Map<String, Object>>> obterParagens() {
        List<Map<String, Object>> paragens = stopRepository.findAll().stream()
            .map(this::toMarcador)
            .collect(Collectors.toList());
        return ResponseEntity.ok(paragens);
    }

    // UC06.1, UC06.2, UC06.3 — Popup LIVE DATA completo
    @GetMapping("/paragens/{stopId}/live-data")
    public ResponseEntity<Map<String, Object>> obterLiveData(@PathVariable String stopId) {
        Optional<Stop> stopOpt = stopRepository.findById(stopId);
        if (stopOpt.isEmpty()) return ResponseEntity.notFound().build();

        // UC06.1 — afluência e distribuição por tipo (O0.5.1.c)
        Map<String, Object> insights = controladorAgregacaoProcura
            .obterInsights(Optional.of(stopId));
        List<Object[]> porTipo = controladorAgregacaoProcura
            .obterContagemPorTipo(Optional.of(stopId));

        Map<String, Long> ticketTypeDistribution = new LinkedHashMap<>();
        for (Object[] row : porTipo) {
            if (row[0] != null) {
                ticketTypeDistribution.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        long totalValidas = insights.containsKey("total")
            ? ((Number) insights.get("total")).longValue() : 0L;

        // UC06.3 — inválidas por paragem (quarentena P7)
        long totalInvalidas  = validationQuarantineRepository.countByOriginStopId(stopId);
        long totalGeral      = totalValidas + totalInvalidas;
        double taxaInvalidas = totalGeral > 0
            ? Math.round((double) totalInvalidas / totalGeral * 10000.0) / 100.0 : 0.0;

        // UC06.3 — top 3 motivos de rejeição
        List<Object[]> topMotivos = validationQuarantineRepository
            .findTopMotivosByOriginStopId(stopId);
        List<Map<String, Object>> top3 = topMotivos.stream().limit(3).map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("motivo", row[0].toString());
            m.put("total",  ((Number) row[1]).longValue());
            return m;
        }).collect(Collectors.toList());

        // UC06.2 — alerta qualidade degradada se taxa > 2%
        boolean qualidadeDegradada = taxaInvalidas > 2.0;

        // Janela de pico
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

        // Campos alinhados com StopLiveDataPopup.js
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("stopId",                                   stopOpt.get().getStopId());
        resposta.put("stopName",                                 stopOpt.get().getStopName());
        resposta.put("peakHourWindowStart",                      peakWindowStart);
        resposta.put("peakHourWindowEnd",                        peakWindowEnd);
        resposta.put("peakHourValidationsCount",                 totalValidas > 0 ? (long)(totalValidas * peakPercentage / 100) : 0L);
        resposta.put("peakHourValidationsPercentageOfStopTotal", peakPercentage);
        resposta.put("invalidValidationsCount",                  totalInvalidas);
        resposta.put("invalidValidationsPercentageOfStopTotal",  taxaInvalidas);
        resposta.put("qualidadeDegradada",                       qualidadeDegradada);
        resposta.put("topMotivoRejeicao",                        top3);
        resposta.put("ticketTypeDistribution",                   ticketTypeDistribution);
        resposta.put("totalValidations",                         totalValidas);

        return ResponseEntity.ok(resposta);
    }

    private Map<String, Object> toMarcador(Stop stop) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("stopId",   stop.getStopId());
        m.put("stopName", stop.getStopName());
        m.put("lat",      stop.getStopLat());
        m.put("lon",      stop.getStopLon());
        return m;
    }
}
