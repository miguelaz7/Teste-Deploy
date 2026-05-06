package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p5_analise_operacional_tempo_real.ControladorAgregacaoProcura;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    public ResponseEntity<List<?>> obterProcuraPorHorario() {
        return ResponseEntity.ok(controladorAgregacaoProcura.obterPorPerspectiva("HORARIO"));
    }

    // Perspectiva por linha — afluência por route_id
    @GetMapping("/por-linha")
    public ResponseEntity<List<?>> obterProcuraPorLinha() {
        return ResponseEntity.ok(controladorAgregacaoProcura.obterPorPerspectiva("LINHA"));
    }

    // Perspectiva por zona/paragem — afluência por stop_id
    @GetMapping("/por-paragem")
    public ResponseEntity<List<?>> obterProcuraPorParagem() {
        return ResponseEntity.ok(controladorAgregacaoProcura.obterPorPerspectiva("ZONA_PARAGEM"));
    }

    // Insights em tempo real com filtro opcional por paragem
    @GetMapping("/tempo-real")
    public ResponseEntity<Map<String, Object>> obterProcuraTempoReal(
        @RequestParam(required = false) String stopId
    ) {
        return ResponseEntity.ok(
            controladorAgregacaoProcura.obterInsights(Optional.ofNullable(stopId)));
    }

    // Distribuição por tipo de título com filtro opcional por paragem
    @GetMapping("/por-tipo")
    public ResponseEntity<List<Object[]>> obterProcuraPorTipo(
        @RequestParam(required = false) String stopId
    ) {
        return ResponseEntity.ok(
            controladorAgregacaoProcura.obterContagemPorTipo(Optional.ofNullable(stopId)));
    }
}
