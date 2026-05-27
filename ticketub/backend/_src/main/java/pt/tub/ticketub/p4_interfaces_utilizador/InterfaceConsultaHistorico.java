package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;
import pt.tub.ticketub.p5_analise_operacional_tempo_real.HistoricoConsolidado;
import pt.tub.ticketub.p5_analise_operacional_tempo_real.RepositorioHistoricoConsolidado;
import pt.tub.ticketub.p5_analise_operacional_tempo_real.ControladorAgregacaoHistorica;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

// =============================================================================
// O010.2.i – Interface de Consulta de Histórico (UC10.2 / UC10.3)
// Interface para a direcção consultar séries temporais, comparar períodos
// e exportar relatórios com metadados de rastreabilidade.
// Consome dados de: O010.1.d (histórico consolidado).
// =============================================================================

@RestController
@RequestMapping("/api/historico")
public class InterfaceConsultaHistorico {

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioHistoricoConsolidado historicoRepository;
    private final RepositorioAuditoriaIngestao auditRepository;
    private final ControladorAgregacaoHistorica historyController;

    public InterfaceConsultaHistorico(
        RepositorioEventoValidacao validationEventRepository,
        RepositorioHistoricoConsolidado historicoRepository,
        RepositorioAuditoriaIngestao auditRepository,
        ControladorAgregacaoHistorica historyController
    ) {
        this.validationEventRepository = validationEventRepository;
        this.historicoRepository       = historicoRepository;
        this.auditRepository           = auditRepository;
        this.historyController         = historyController;
    }

    // 1. Obter séries temporais (UC10.2)
    @GetMapping("/series-temporais")
    public ResponseEntity<List<HistoricoConsolidado>> getSeriesTemporais(
        @RequestParam(required = false) String routeId,
        @RequestParam(required = false) String perfilTarifario,
        @RequestParam(defaultValue = "DIA") String granularidade,
        @RequestParam String inicio,
        @RequestParam String fim
    ) {
        LocalDate start = LocalDate.parse(inicio);
        LocalDate end   = LocalDate.parse(fim);

        List<HistoricoConsolidado> records = historicoRepository.findAll().stream()
            .filter(h -> h.getGranularidade().equalsIgnoreCase(granularidade))
            .filter(h -> !h.getPeriodoInicio().isBefore(start) && !h.getPeriodoFim().isAfter(end))
            .filter(h -> routeId == null || routeId.isBlank() || Objects.equals(h.getRouteId(), routeId))
            .filter(h -> perfilTarifario == null || perfilTarifario.isBlank() || Objects.equals(h.getPerfilTarifario(), perfilTarifario))
            .sorted(Comparator.comparing(HistoricoConsolidado::getPeriodoInicio))
            .collect(Collectors.toList());

        return ResponseEntity.ok(records);
    }

    // 2. Comparação detalhada entre dois períodos customizados (UC10.2)
    @GetMapping("/comparacao-periodos")
    public ResponseEntity<Map<String, Object>> compararPeriodos(
        @RequestParam String inicio,
        @RequestParam String fim,
        @RequestParam(required = false) String compInicio,
        @RequestParam(required = false) String compFim
    ) {
        LocalDate p1Start = LocalDate.parse(inicio);
        LocalDate p1End   = LocalDate.parse(fim);

        LocalDate p0Start;
        LocalDate p0End;

        if (compInicio != null && compFim != null) {
            p0Start = LocalDate.parse(compInicio);
            p0End   = LocalDate.parse(compFim);
        } else {
            // Se omitido, calcula período anterior homólogo de mesmo tamanho
            long durationDays = ChronoUnit.DAYS.between(p1Start, p1End) + 1;
            p0Start = p1Start.minusDays(durationDays);
            p0End   = p1Start.minusDays(1);
        }

        // Buscar todos os consolidados para cálculo
        List<HistoricoConsolidado> todos = historicoRepository.findAll();

        long p1Validacoes = 0;
        BigDecimal p1Receita = BigDecimal.ZERO;
        long p0Validacoes = 0;
        BigDecimal p0Receita = BigDecimal.ZERO;

        for (HistoricoConsolidado hc : todos) {
            if ("DIA".equalsIgnoreCase(hc.getGranularidade())) {
                LocalDate d = hc.getPeriodoInicio();
                if (!d.isBefore(p1Start) && !d.isAfter(p1End)) {
                    p1Validacoes += hc.getTotalValidacoes();
                    if (hc.getReceitaEstimada() != null) {
                        p1Receita = p1Receita.add(hc.getReceitaEstimada());
                    }
                } else if (!d.isBefore(p0Start) && !d.isAfter(p0End)) {
                    p0Validacoes += hc.getTotalValidacoes();
                    if (hc.getReceitaEstimada() != null) {
                        p0Receita = p0Receita.add(hc.getReceitaEstimada());
                    }
                }
            }
        }

        double varValidacoes = p0Validacoes > 0
            ? ((double) (p1Validacoes - p0Validacoes) / p0Validacoes) * 100.0
            : 0.0;

        double varReceita = p0Receita.compareTo(BigDecimal.ZERO) > 0
            ? p1Receita.subtract(p0Receita)
                .multiply(new BigDecimal("100"))
                .divide(p0Receita, 2, RoundingMode.HALF_UP)
                .doubleValue()
            : 0.0;

        // Resumo textual
        String direcaoStr = varValidacoes >= 0 ? "Aumento" : "Queda";
        String resumoTextual = String.format("%s de %.2f%% nas validações e %.2f%% na receita estimada vs. período anterior.",
            direcaoStr, Math.abs(varValidacoes), Math.abs(varReceita));

        Map<String, Object> period1Info = new LinkedHashMap<>();
        period1Info.put("inicio", p1Start);
        period1Info.put("fim", p1End);
        period1Info.put("validacoes", p1Validacoes);
        period1Info.put("receita", p1Receita);

        Map<String, Object> period0Info = new LinkedHashMap<>();
        period0Info.put("inicio", p0Start);
        period0Info.put("fim", p0End);
        period0Info.put("validacoes", p0Validacoes);
        period0Info.put("receita", p0Receita);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("periodoAtual", period1Info);
        resposta.put("periodoComparacao", period0Info);
        resposta.put("variacaoValidacoes", Math.round(varValidacoes * 100.0) / 100.0);
        resposta.put("variacaoReceita", Math.round(varReceita * 100.0) / 100.0);
        resposta.put("resumoTextual", resumoTextual);

        return ResponseEntity.ok(resposta);
    }

    // 3. Exportar relatório histórico com rastreabilidade de DPO (UC10.3)
    @GetMapping("/exportar")
    public ResponseEntity<?> exportarRelatorio(
        @RequestParam String inicio,
        @RequestParam String fim,
        @RequestParam(defaultValue = "CSV") String formato,
        @RequestParam(required = false) String routeId,
        @RequestHeader(value = "X-Api-User", defaultValue = "analista") String user
    ) {
        // Validar permissão
        if (user == null || (!user.equals("analista") && !user.equals("admin") && !user.equals("dpo") && !user.equals("gestor"))) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado: utilizador sem permissão contratada com o DPO para exportar relatórios históricos."));
        }

        LocalDate start = LocalDate.parse(inicio);
        LocalDate end   = LocalDate.parse(fim);

        List<HistoricoConsolidado> records = historicoRepository.findAll().stream()
            .filter(h -> "DIA".equalsIgnoreCase(h.getGranularidade()))
            .filter(h -> !h.getPeriodoInicio().isBefore(start) && !h.getPeriodoFim().isAfter(end))
            .filter(h -> routeId == null || routeId.isBlank() || Objects.equals(h.getRouteId(), routeId))
            .collect(Collectors.toList());

        // Snapshot final para exportar
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (HistoricoConsolidado h : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("data", h.getPeriodoInicio());
            item.put("linha", h.getRouteId() != null ? h.getRouteId() : "Geral");
            item.put("perfil", h.getPerfilTarifario() != null ? h.getPerfilTarifario() : "Geral");
            item.put("validacoes", h.getTotalValidacoes());
            item.put("receita_estimada", h.getReceitaEstimada());
            snapshot.add(item);
        }

        // Auditar a exportação
        auditRepository.save(new RegistoAuditoriaIngestao(
            "EXPORT_HISTORICO",
            "utilizador",
            user,
            String.format("Exportação do Histórico de Validações em formato %s. Período: %s a %s. Total de registos: %d.",
                formato, inicio, fim, snapshot.size()),
            OffsetDateTime.now()
        ));

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("exportTime", OffsetDateTime.now());
        output.put("formato", formato);
        output.put("user", user);
        output.put("periodo", start + " a " + end);
        output.put("dados", snapshot);

        return ResponseEntity.ok(output);
    }

    // 4. Forçar reprocessamento de partição/período em falha (FA1 / Recovery)
    @PostMapping("/reprocessar")
    public ResponseEntity<Map<String, Object>> reprocessarPeriodo(
        @RequestParam String inicio,
        @RequestParam String fim
    ) {
        LocalDate start = LocalDate.parse(inicio);
        LocalDate end   = LocalDate.parse(fim);

        try {
            historyController.reprocessRange(start, end);
            return ResponseEntity.ok(Map.of(
                "status", "SUCESSO",
                "mensagem", String.format("Reprocessamento executado e consolidado para o intervalo %s a %s.", start, end)
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "FALHA",
                "erro", e.getMessage()
            ));
        }
    }
}
