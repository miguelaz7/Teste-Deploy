package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p5_analise_operacional_tempo_real.ControladorAgregacaoProcura;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.web.bind.annotation.PathVariable;
import pt.tub.ticketub.p5_analise_operacional_tempo_real.AgregadoProcura;
import java.util.stream.Collectors;

// =============================================================================
// O0.5.3.i – Interface de Procura em Tempo Real (UC05.3)
// Apresenta as perspectivas de análise (por horário, linha e zona/paragem)
// com filtros e actualização contínua.
// Consome dados de: O0.5.1.c e O0.5.1.d (repositório analítico de procura).
// =============================================================================

@RestController
@RequestMapping("/api/procura")
public class InterfaceProcuraTempoReal {

    private final ControladorAgregacaoProcura controladorAgregacaoProcura;

    public InterfaceProcuraTempoReal(ControladorAgregacaoProcura controladorAgregacaoProcura) {
        this.controladorAgregacaoProcura = controladorAgregacaoProcura;
    }

    // Perspectiva por horário — afluência por hora do dia
    @GetMapping("/por-horario")
    public ResponseEntity<List<?>> getDemandBySchedule() {
        return ResponseEntity.ok(controladorAgregacaoProcura.getByPerspective("HORARIO"));
    }

    // Perspectiva por linha — afluência por route_id
    @GetMapping("/por-linha")
    public ResponseEntity<List<?>> getDemandByRoute() {
        return ResponseEntity.ok(controladorAgregacaoProcura.getByPerspective("LINHA"));
    }

    // Top 10 linhas com procura mais elevada (UC05.2)
    @GetMapping("/por-linha/top-10")
    public ResponseEntity<List<AgregadoProcura>> getTop10Routes() {
        List<AgregadoProcura> todas = controladorAgregacaoProcura.getByPerspective("LINHA");
        List<AgregadoProcura> top10 = todas.stream().limit(10).collect(Collectors.toList());
        return ResponseEntity.ok(top10);
    }

    // Drill-Down: obter detalhe horário de uma linha específica (UC05.2)
    @GetMapping("/por-linha/{routeId}/horario")
    public ResponseEntity<Map<Integer, Long>> getRouteHourlyDetail(@PathVariable String routeId) {
        return ResponseEntity.ok(controladorAgregacaoProcura.getHourlyDetailForRoute(routeId));
    }

    // Perspectiva por zona/paragem — afluência por stop_id
    @GetMapping("/por-paragem")
    public ResponseEntity<List<?>> getDemandByStop() {
        return ResponseEntity.ok(controladorAgregacaoProcura.getByPerspective("ZONA_PARAGEM"));
    }

    // Perspectiva por zona — afluência agregada por zona geográfica (UC05.3)
    @GetMapping("/por-zona")
    public ResponseEntity<List<?>> getDemandByZone() {
        return ResponseEntity.ok(controladorAgregacaoProcura.getByPerspective("ZONA"));
    }

    // Endpoint para forçar a agregação de dados manualmente (UC05)
    @PostMapping("/aggregate")
    public ResponseEntity<Void> triggerAggregation() {
        controladorAgregacaoProcura.aggregate();
        return ResponseEntity.ok().build();
    }

    // Insights em tempo real com filtro opcional por paragem
    @GetMapping("/tempo-real")
    public ResponseEntity<Map<String, Object>> getRealTimeDemand(
        @RequestParam(required = false) String stopId
    ) {
        return ResponseEntity.ok(
            controladorAgregacaoProcura.getInsights(Optional.ofNullable(stopId)));
    }

    // Distribuição por tipo de título com filtro opcional por paragem
    @GetMapping("/por-tipo")
    public ResponseEntity<List<Object[]>> getDemandByType(
        @RequestParam(required = false) String stopId
    ) {
        return ResponseEntity.ok(
            controladorAgregacaoProcura.getCountByType(Optional.ofNullable(stopId)));
    }
}
