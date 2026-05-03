package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p5_analise_operacional_tempo_real.ValidationInsightsService;
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

    private final ValidationInsightsService validationInsightsService;

    public InterfaceProcuraTempoReal(ValidationInsightsService validationInsightsService) {
        this.validationInsightsService = validationInsightsService;
    }

    // Perspectiva por horário e afluência geral, com filtro opcional por paragem
    @GetMapping("/tempo-real")
    public ResponseEntity<Map<String, Object>> obterProcuraTempoReal(
        @RequestParam(required = false) String stopId
    ) {
        Map<String, Object> dados = validationInsightsService.obterInsights(Optional.ofNullable(stopId));
        return ResponseEntity.ok(dados);
    }

    // Perspectiva por perfil tarifário (distribuição por tipo de título)
    @GetMapping("/por-tipo")
    public ResponseEntity<List<Object[]>> obterProcuraPorTipo(
        @RequestParam(required = false) String stopId
    ) {
        List<Object[]> dados = validationInsightsService.obterContagemPorTipo(Optional.ofNullable(stopId));
        return ResponseEntity.ok(dados);
    }
}
