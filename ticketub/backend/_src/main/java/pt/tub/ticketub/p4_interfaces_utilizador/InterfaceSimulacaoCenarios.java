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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// =============================================================================
// O011.1.i – Interface de Simulação de Cenários (UC11.1)
// Permite ao gestor definir cenários de ajuste, visualizar projecções
// com nível de confiança e guardar/partilhar os cenários.
// Consome dados de: O011.1.c (controlador de simulação).
// =============================================================================

@RestController
@RequestMapping("/api/simulacao")
public class InterfaceSimulacaoCenarios {

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioRota routeRepository;

    public InterfaceSimulacaoCenarios(RepositorioEventoValidacao validationEventRepository,
                                      RepositorioRota routeRepository) {
        this.validationEventRepository = validationEventRepository;
        this.routeRepository = routeRepository;
    }

    // Projeção de impacto de ajuste numa linha com base no histórico
    @GetMapping("/projecao")
    public ResponseEntity<Map<String, Object>> obterProjecao(
        @RequestParam String routeId,
        @RequestParam(defaultValue = "0") double ajustePercent,
        @RequestParam(defaultValue = "30") int diasHistorico
    ) {
        OffsetDateTime desde = OffsetDateTime.now().minusDays(diasHistorico);
        List<Object[]> contagens = validationEventRepository.countByRouteIdAfter(desde);

        long totalHistorico = contagens.stream()
            .filter(r -> routeId.equals(String.valueOf(r[0])))
            .mapToLong(r -> ((Number) r[1]).longValue())
            .findFirst().orElse(0L);

        double mediadiaria = diasHistorico > 0
            ? (double) totalHistorico / diasHistorico : 0.0;
        double projecao = mediadiaria * (1 + ajustePercent / 100.0);

        // Nível de confiança: degradado se histórico < 7 dias
        String nivelConfianca = diasHistorico >= 7 ? "ALTO" : "BAIXO";

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("routeId",          routeId);
        resposta.put("ajustePercent",    ajustePercent);
        resposta.put("mediaDiaria",      BigDecimal.valueOf(mediadiaria).setScale(2, RoundingMode.HALF_UP));
        resposta.put("projecaoDiaria",   BigDecimal.valueOf(projecao).setScale(2, RoundingMode.HALF_UP));
        resposta.put("nivelConfianca",   nivelConfianca);
        resposta.put("diasHistorico",    diasHistorico);

        return ResponseEntity.ok(resposta);
    }
}