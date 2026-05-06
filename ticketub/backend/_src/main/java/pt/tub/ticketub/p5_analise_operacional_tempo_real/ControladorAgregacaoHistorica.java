package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O010.1.c – Controlador de Agregação Histórica
// Actualiza continuamente os agregados históricos a partir dos eventos
// ingeridos. Em falha, regista o intervalo e tenta recuperação automática.
// Agendado às 23h30 para consolidação diária (UC10).
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
class ControladorAgregacaoHistorica {

    private final ValidationEventRepository validationEventRepository;
    private final HistoricoConsolidadoRepository historicoRepository;
    private final DesvioOperacionalRepository desvioRepository;

    ControladorAgregacaoHistorica(
        ValidationEventRepository validationEventRepository,
        HistoricoConsolidadoRepository historicoRepository,
        DesvioOperacionalRepository desvioRepository
    ) {
        this.validationEventRepository = validationEventRepository;
        this.historicoRepository       = historicoRepository;
        this.desvioRepository          = desvioRepository;
    }

    // UC10.1: actualização incremental contínua a cada 5 minutos
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void actualizarIncremental() {
        LocalDate hoje = LocalDate.now();
        List<ValidationEvent> eventos = validationEventRepository.findAll();
        if (eventos.isEmpty()) return;
        consolidarPorLinha(eventos, hoje);
        consolidarPorPerfil(eventos, hoje);
    }

    // Consolidação diária completa às 23h30 (UC10)
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void consolidarDiario() {
        LocalDate hoje = LocalDate.now();
        List<ValidationEvent> eventos = validationEventRepository.findAll();
        if (eventos.isEmpty()) return;

        consolidarPorLinha(eventos, hoje);
        consolidarPorPerfil(eventos, hoje);
        calcularDesviosOperacionais(eventos, hoje);
    }

    // -------------------------------------------------------------------------
    // Consolidação por linha
    // -------------------------------------------------------------------------

    private void consolidarPorLinha(List<ValidationEvent> eventos, LocalDate data) {
        Map<String, List<ValidationEvent>> porLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(ValidationEvent::getRouteId));

        for (Map.Entry<String, List<ValidationEvent>> entrada : porLinha.entrySet()) {
            String routeId = entrada.getKey();
            List<ValidationEvent> grupo = entrada.getValue();
            long total = grupo.size();

            // Receita estimada — soma de fareForAdult não nulos
            BigDecimal receita = grupo.stream()
                .filter(e -> e.getFareForAdult() != null)
                .map(ValidationEvent::getFareForAdult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            guardarHistorico(routeId, null, "DIA", data, data, total, receita, 0.0);
        }
    }

    // -------------------------------------------------------------------------
    // Consolidação por perfil tarifário
    // -------------------------------------------------------------------------

    private void consolidarPorPerfil(List<ValidationEvent> eventos, LocalDate data) {
        Map<String, List<ValidationEvent>> porPerfil = eventos.stream()
            .filter(e -> e.getPerfilClassificado() != null)
            .collect(Collectors.groupingBy(ValidationEvent::getPerfilClassificado));

        for (Map.Entry<String, List<ValidationEvent>> entrada : porPerfil.entrySet()) {
            String perfil = entrada.getKey();
            long total = entrada.getValue().size();
            guardarHistorico(null, perfil, "DIA", data, data, total, BigDecimal.ZERO, 0.0);
        }
    }

    // -------------------------------------------------------------------------
    // Cálculo de desvios operacionais por linha
    // -------------------------------------------------------------------------

    private void calcularDesviosOperacionais(List<ValidationEvent> eventos, LocalDate hoje) {
        Map<String, Long> realizadosPorLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(ValidationEvent::getRouteId, Collectors.counting()));

        for (Map.Entry<String, Long> entrada : realizadosPorLinha.entrySet()) {
            String routeId = entrada.getKey();
            long realizados = entrada.getValue();

            // Média histórica: últimas 4 semanas (mesmo dia da semana)
            LocalDate h4semanas = hoje.minusWeeks(4);
            List<HistoricoConsolidado> historico = historicoRepository
                .findByRouteIdAndPerfilTarifarioAndGranularidadeAndPeriodoInicio(
                    routeId, null, "DIA", h4semanas)
                .map(List::of).orElse(List.of());

            double media = historico.isEmpty()
                ? realizados
                : historico.stream()
                    .mapToLong(HistoricoConsolidado::getTotalValidacoes)
                    .average().orElse(realizados);

            double desvio = media > 0
                ? Math.round((realizados - media) / media * 10000.0) / 100.0
                : 0.0;

            boolean critico = Math.abs(desvio) > 20.0;

            desvioRepository.save(new DesvioOperacional(
                routeId, hoje, "DIA_COMPLETO",
                realizados, media, desvio, critico, OffsetDateTime.now()
            ));
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void guardarHistorico(String routeId, String perfil, String granularidade,
                                   LocalDate inicio, LocalDate fim, long total,
                                   BigDecimal receita, double taxaAnomalias) {
        Optional<HistoricoConsolidado> existente = historicoRepository
            .findByRouteIdAndPerfilTarifarioAndGranularidadeAndPeriodoInicio(
                routeId, perfil, granularidade, inicio);

        HistoricoConsolidado hc = existente.orElse(new HistoricoConsolidado(
            routeId, perfil, granularidade, inicio, fim,
            0, BigDecimal.ZERO, 0.0, OffsetDateTime.now()
        ));

        hc.setTotalValidacoes(total);
        hc.setReceitaEstimada(receita);
        hc.setTaxaAnomalias(taxaAnomalias);
        hc.setActualizadoEm(OffsetDateTime.now());
        historicoRepository.save(hc);
    }
}
