package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRota;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// =============================================================================
// O0.7.2.i – Interface de Análise de Desvio Operacional (UC07.2)
// Apresenta ao gestor de exploração as linhas com desvio de procura face
// à média histórica, com destaque das linhas críticas.
// Consome dados de: O0.7.2.d (desvios operacionais) e O0.7.1.d (correlações GPS).
// =============================================================================

@RestController
@RequestMapping("/api/desvio-operacional")
public class InterfaceDesvioOperacional {

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioRota routeRepository;

    public InterfaceDesvioOperacional(RepositorioEventoValidacao validationEventRepository,
                                      RepositorioRota routeRepository) {
        this.validationEventRepository = validationEventRepository;
        this.routeRepository = routeRepository;
    }

    // Desvio de procura por linha: validações recentes vs. média histórica
    @GetMapping("/por-linha")
    public ResponseEntity<List<Map<String, Object>>> obterDesviosPorLinha(
        @RequestParam(defaultValue = "7") int diasHistorico,
        @RequestParam(defaultValue = "1") int diasRecentes
    ) {
        OffsetDateTime agora = OffsetDateTime.now();
        OffsetDateTime inicioRecente   = agora.minusDays(diasRecentes);
        OffsetDateTime inicioHistorico = agora.minusDays(diasHistorico);

        List<Object[]> recentes   = validationEventRepository.countByRouteIdAfter(inicioRecente);
        List<Object[]> historicos = validationEventRepository.countByRouteIdAfter(inicioHistorico);

        Map<String, Long> mapaRecente   = toMap(recentes);
        Map<String, Long> mapaHistorico = toMap(historicos);

        List<Map<String, Object>> resultado = new ArrayList<>();

        for (Map.Entry<String, Long> entrada : mapaRecente.entrySet()) {
            String routeId = entrada.getKey();
            long totalRecente = entrada.getValue();

            long totalHistorico = mapaHistorico.getOrDefault(routeId, 0L);
            double mediaHistorica = diasHistorico > 0
                ? (double) totalHistorico / diasHistorico
                : 0.0;
            double mediaRecente = diasRecentes > 0
                ? (double) totalRecente / diasRecentes
                : 0.0;

            double desvioPercentual = mediaHistorica > 0
                ? BigDecimal.valueOf((mediaRecente - mediaHistorica) / mediaHistorica * 100)
                    .setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("routeId",          routeId);
            linha.put("totalRecente",     totalRecente);
            linha.put("mediaHistorica",   BigDecimal.valueOf(mediaHistorica)
                .setScale(2, RoundingMode.HALF_UP).doubleValue());
            linha.put("desvioPercentual", desvioPercentual);
            linha.put("critico",          Math.abs(desvioPercentual) > 20.0);
            resultado.add(linha);
        }

        // Ordenar por desvio absoluto descendente (linhas críticas primeiro)
        resultado.sort((a, b) -> Double.compare(
            Math.abs((double) b.get("desvioPercentual")),
            Math.abs((double) a.get("desvioPercentual"))
        ));

        return ResponseEntity.ok(resultado);
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> mapa = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) {
                mapa.put(row[0].toString(), ((Number) row[1]).longValue());
            }
        }
        return mapa;
    }
}