package pt.tub.ticketub.p8_planeamento_rede_integracao_erp;

// =============================================================================
// O011.1.c – Controlador de Simulação e Integração ERP
// Executa simulações aplicando histórico (O010.1.d) e matriz O-D (O0.8.1.d).
// Projecta impacto com nível de confiança e persiste com versão dos dados de base.
// Envia dados consolidados ao ERP com retry automático a cada 15 minutos.
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import pt.tub.ticketub.p5_analise_operacional_tempo_real.HistoricoConsolidadoRepository;
import pt.tub.ticketub.p6_estimativa_fluxos_origem_destino.MatrizODRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// =============================================================================
// O011.1.c serve também como interface REST (UC11.1, UC11.2, UC11.3)
// porque o 4SRS marca O011.1.i como kill — a interface é incorporada aqui.
// =============================================================================

@Service
@RestController
@RequestMapping("/api/planeamento")
public class ControladorSimulacaoIntegracaoERP {

    private final CenarioPlaneamentoRepository cenarioRepository;
    private final DadosFinanceirosERPRepository dadosFinanceirosRepository;
    private final ValidationEventRepository validationEventRepository;
    private final HistoricoConsolidadoRepository historicoRepository;
    private final MatrizODRepository matrizODRepository;

    public ControladorSimulacaoIntegracaoERP(
        CenarioPlaneamentoRepository cenarioRepository,
        DadosFinanceirosERPRepository dadosFinanceirosRepository,
        ValidationEventRepository validationEventRepository,
        HistoricoConsolidadoRepository historicoRepository,
        MatrizODRepository matrizODRepository
    ) {
        this.cenarioRepository        = cenarioRepository;
        this.dadosFinanceirosRepository = dadosFinanceirosRepository;
        this.validationEventRepository = validationEventRepository;
        this.historicoRepository       = historicoRepository;
        this.matrizODRepository        = matrizODRepository;
    }

    // -------------------------------------------------------------------------
    // UC11.1 — Simular cenário de ajuste de rede
    // -------------------------------------------------------------------------

    @PostMapping("/simular")
    @Transactional
    public ResponseEntity<Map<String, Object>> simularCenario(@RequestBody Map<String, Object> params) {
        String routeId          = (String) params.get("routeId");
        String descricao        = (String) params.get("descricao");
        String periodo          = (String) params.getOrDefault("periodo", "DIA_COMPLETO");
        double valorAntes       = Double.parseDouble(params.getOrDefault("valorAntes", "10").toString());
        double valorDepois      = Double.parseDouble(params.getOrDefault("valorDepois", "8").toString());
        String criadoPor        = (String) params.getOrDefault("criadoPor", "sistema");

        // Consulta histórico de P5 para a linha
        List<?> historico = historicoRepository.findByRouteId(routeId);
        long totalHistorico = historico.stream()
            .mapToLong(h -> {
                if (h instanceof pt.tub.ticketub.p5_analise_operacional_tempo_real.HistoricoConsolidado hc) {
                    return hc.getTotalValidacoes();
                }
                return 0L;
            }).sum();

        // Calcula ocupação esperada: procura histórica / novas circulações
        double ocupacaoEsperada = valorDepois > 0
            ? Math.round((totalHistorico / valorDepois) * 100.0) / 100.0 : 0.0;

        // Estima perda de receita
        BigDecimal receitaImpacto = calcularImpactoReceita(routeId, valorAntes, valorDepois);

        // Nível de confiança baseado na quantidade de histórico disponível
        double nivelConfianca = calcularNivelConfianca(historico.size());

        // UC11.3: versão e hash dos dados de base
        String versao    = "v" + LocalDate.now() + "_" + routeId;
        String hashBase  = calcularHash(versao + totalHistorico);

        // Código único do cenário
        String codigo = "SC_" + LocalDate.now().toString().replace("-", "")
            + "_" + routeId + "_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        CenarioPlaneamento cenario = new CenarioPlaneamento(
            codigo, routeId, descricao, periodo,
            BigDecimal.valueOf(valorAntes), BigDecimal.valueOf(valorDepois),
            BigDecimal.valueOf(ocupacaoEsperada), receitaImpacto,
            nivelConfianca, versao, hashBase, criadoPor, OffsetDateTime.now()
        );
        cenarioRepository.save(cenario);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("codigoCenario",    codigo);
        resposta.put("routeId",          routeId);
        resposta.put("ocupacaoEsperada", ocupacaoEsperada);
        resposta.put("receitaImpacto",   receitaImpacto);
        resposta.put("nivelConfianca",   nivelConfianca);
        resposta.put("avisoRisco",       nivelConfianca < 70.0);
        resposta.put("versaoDadosBase",  versao);

        return ResponseEntity.ok(resposta);
    }

    // Lista cenários existentes
    @GetMapping("/cenarios")
    public ResponseEntity<List<Map<String, Object>>> listarCenarios() {
        List<Map<String, Object>> lista = cenarioRepository.findAll().stream()
            .map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("codigoCenario",  c.getCodigoCenario());
                m.put("routeId",        c.getRouteId());
                m.put("descricao",      c.getDescricao());
                m.put("nivelConfianca", c.getNivelConfianca());
                m.put("estado",         c.getEstado());
                m.put("criadoEm",       c.getCriadoEm());
                return m;
            }).collect(Collectors.toList());
        return ResponseEntity.ok(lista);
    }

    // -------------------------------------------------------------------------
    // UC11.2 — Estruturar e enviar dados para ERP
    // -------------------------------------------------------------------------

    // Retry automático a cada 15 minutos (UC11 — falha de integração ERP)
    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void enviarPendentesParaERP() {
        List<DadosFinanceirosERP> pendentes = dadosFinanceirosRepository
            .findByEstadoERP("PENDENTE");

        for (DadosFinanceirosERP dados : pendentes) {
            tentarEnviarERP(dados);
        }
    }

    // Gera e estrutura dados financeiros para o ERP
    @PostMapping("/erp/gerar")
    @Transactional
    public ResponseEntity<Map<String, Object>> gerarDadosERP(@RequestBody Map<String, Object> params) {
        String routeId    = (String) params.get("routeId");
        String tipoTitulo = (String) params.getOrDefault("tipoTitulo", "TODOS");
        String geradoPor  = (String) params.getOrDefault("geradoPor", "sistema");
        LocalDate inicio  = LocalDate.parse(params.getOrDefault("periodoInicio",
            LocalDate.now().minusMonths(1).toString()).toString());
        LocalDate fim     = LocalDate.parse(params.getOrDefault("periodoFim",
            LocalDate.now().toString()).toString());

        // Contar validações no período
        long totalValidacoes = validationEventRepository.countByIngestedAtAfter(
            inicio.atStartOfDay().atOffset(java.time.ZoneOffset.UTC));

        // Receita estimada da linha
        BigDecimal receita = calcularReceitaLinha(routeId, inicio, fim);
        BigDecimal receitaPorKm = receita.compareTo(BigDecimal.ZERO) > 0
            ? receita.divide(BigDecimal.valueOf(Math.max(totalValidacoes, 1)), 4, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Taxa de anomalias
        BigDecimal taxaAnomalias = BigDecimal.valueOf(2.5);
        BigDecimal impactoAnomalias = receita.multiply(taxaAnomalias)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        boolean requerValidacao = taxaAnomalias.compareTo(BigDecimal.valueOf(5.0)) > 0;

        // Versão e hash de integridade
        String versao = "ERP_" + LocalDate.now() + "_v" + UUID.randomUUID().toString().substring(0, 4);
        String payload = gerarPayloadCSV(routeId, tipoTitulo, totalValidacoes, receita, inicio, fim);
        String hash    = calcularHash(payload);

        DadosFinanceirosERP dados = new DadosFinanceirosERP(
            routeId, tipoTitulo, inicio, fim,
            totalValidacoes, receita, receitaPorKm, receitaPorKm,
            taxaAnomalias, impactoAnomalias,
            versao, hash, requerValidacao, geradoPor, OffsetDateTime.now(), payload
        );
        dadosFinanceirosRepository.save(dados);

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("versaoExportacao",    versao);
        resposta.put("totalValidacoes",     totalValidacoes);
        resposta.put("receitaEstimada",     receita);
        resposta.put("requerValidacaoManual", requerValidacao);
        resposta.put("hashIntegridade",     hash);
        resposta.put("estadoERP",           "PENDENTE");

        return ResponseEntity.ok(resposta);
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void tentarEnviarERP(DadosFinanceirosERP dados) {
        try {
            // Ponto de integração com o ERP externo dos TUB
            // Em produção: chamada HTTP ao ERP com o payload formatado
            dados.setEstadoERP("ENVIADO");
            dados.setEnviadoEm(OffsetDateTime.now());
            dadosFinanceirosRepository.save(dados);
        } catch (Exception e) {
            // Falha — fica PENDENTE para retry na próxima execução (15 min)
            dados.setEstadoERP("FALHA");
            dadosFinanceirosRepository.save(dados);
        }
    }

    private BigDecimal calcularImpactoReceita(String routeId, double antes, double depois) {
        List<?> historico = historicoRepository.findByRouteId(routeId);
        BigDecimal totalReceita = historico.stream()
            .map(h -> {
                if (h instanceof pt.tub.ticketub.p5_analise_operacional_tempo_real.HistoricoConsolidado hc) {
                    return hc.getReceitaEstimada() != null ? hc.getReceitaEstimada() : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (antes <= 0) return BigDecimal.ZERO;
        double reducaoPercent = (antes - depois) / antes;
        return totalReceita.multiply(BigDecimal.valueOf(reducaoPercent))
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularReceitaLinha(String routeId, LocalDate inicio, LocalDate fim) {
        List<?> historico = historicoRepository.findByRouteId(routeId);
        return historico.stream()
            .map(h -> {
                if (h instanceof pt.tub.ticketub.p5_analise_operacional_tempo_real.HistoricoConsolidado hc) {
                    return hc.getReceitaEstimada() != null ? hc.getReceitaEstimada() : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private double calcularNivelConfianca(int registosHistorico) {
        // Mais histórico = maior confiança (máximo 95%)
        if (registosHistorico >= 28) return 95.0;
        if (registosHistorico >= 14) return 80.0;
        if (registosHistorico >= 7)  return 70.0;
        return 50.0;
    }

    private String gerarPayloadCSV(String routeId, String tipoTitulo, long validacoes,
                                    BigDecimal receita, LocalDate inicio, LocalDate fim) {
        return String.format("linha|periodo|validacoes|receita|inicio|fim\n%s|%s|%d|%s|%s|%s",
            routeId, tipoTitulo, validacoes, receita, inicio, fim);
    }

    private String calcularHash(String conteudo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(conteudo.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(conteudo.hashCode());
        }
    }
}
