package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p5_analise_operacional_tempo_real.ValidationInsightsService;
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
// O0.6.1.i – Interface do Mapa de Rede (UC06.1)
// Mapa interactivo com marcadores das paragens. Ao clicar, abre popup
// LIVE DATA com afluência, validações inválidas e distribuição por perfil
// tarifário. Consome dados de: O0.5.1.c e O0.5.1.d.
// =============================================================================

@RestController
@RequestMapping("/api/mapa")
public class InterfaceMapaRede {

    private final StopRepository stopRepository;
    private final ValidationInsightsService validationInsightsService;

    public InterfaceMapaRede(StopRepository stopRepository,
                             ValidationInsightsService validationInsightsService) {
        this.stopRepository = stopRepository;
        this.validationInsightsService = validationInsightsService;
    }

    // Lista todas as paragens com coordenadas para marcadores no mapa
    @GetMapping("/paragens")
    public ResponseEntity<List<Map<String, Object>>> obterParagens() {
        List<Map<String, Object>> paragens = stopRepository.findAll().stream()
            .map(this::toMarcador)
            .collect(Collectors.toList());
        return ResponseEntity.ok(paragens);
    }

    // Popup LIVE DATA de uma paragem específica — campos alinhados com StopLiveDataPopup.js
    @GetMapping("/paragens/{stopId}/live-data")
    public ResponseEntity<Map<String, Object>> obterLiveData(@PathVariable String stopId) {
        Optional<Stop> stopOpt = stopRepository.findById(stopId);
        if (stopOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> insights = validationInsightsService
            .obterInsights(Optional.of(stopId));
        List<Object[]> porTipo = validationInsightsService
            .obterContagemPorTipo(Optional.of(stopId));

        // Distribuição por tipo de título no formato Map<String,Long> esperado pelo frontend
        Map<String, Long> ticketTypeDistribution = new LinkedHashMap<>();
        for (Object[] row : porTipo) {
            if (row[0] != null) {
                ticketTypeDistribution.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }

        long totalValidations = insights.containsKey("total")
            ? ((Number) insights.get("total")).longValue() : 0L;
        long invalidCount = insights.containsKey("invalidCount")
            ? ((Number) insights.get("invalidCount")).longValue() : 0L;
        double invalidPercentage = insights.containsKey("invalidPercentage")
            ? ((Number) insights.get("invalidPercentage")).doubleValue() : 0.0;

        // Janela de pico: timeGap vem como OffsetDateTime string, extraímos a hora
        String peakWindowStart = "";
        String peakWindowEnd   = "";
        double peakPercentage  = 0.0;
        if (insights.containsKey("timeGap") && insights.get("timeGap") != null) {
            String timeGap = insights.get("timeGap").toString();
            // formato: "2025-01-01T08:00:00Z" — extraímos HH:mm
            peakWindowStart = timeGap.length() >= 16 ? timeGap.substring(11, 16) : timeGap;
            int startHour = peakWindowStart.length() >= 2
                ? Integer.parseInt(peakWindowStart.substring(0, 2)) : 0;
            peakWindowEnd = String.format("%02d:00", (startHour + 1) % 24);
        }
        if (insights.containsKey("peakAfluenciaPercentage")) {
            peakPercentage = ((Number) insights.get("peakAfluenciaPercentage")).doubleValue();
        }

        // Campos exatamente como StopLiveDataPopup.js os espera
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("stopId",                                    stopOpt.get().getStopId());
        resposta.put("stopName",                                  stopOpt.get().getStopName());
        resposta.put("peakHourWindowStart",                       peakWindowStart);
        resposta.put("peakHourWindowEnd",                         peakWindowEnd);
        resposta.put("peakHourValidationsCount",                  totalValidations > 0 ? (long)(totalValidations * peakPercentage / 100) : 0L);
        resposta.put("peakHourValidationsPercentageOfStopTotal",  peakPercentage);
        resposta.put("invalidValidationsCount",                   invalidCount);
        resposta.put("invalidValidationsPercentageOfStopTotal",   invalidPercentage);
        resposta.put("ticketTypeDistribution",                    ticketTypeDistribution);
        resposta.put("totalValidations",                          totalValidations);

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