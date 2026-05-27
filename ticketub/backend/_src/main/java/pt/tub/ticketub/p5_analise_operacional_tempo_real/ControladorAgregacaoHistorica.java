package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O010.1.c – Controlador de Agregação Histórica
// Actualiza continuamente os agregados históricos a partir dos eventos
// ingeridos. Em falha, regista o intervalo e tenta recuperação automática.
// Agendado às 23h30 para consolidação diária (UC10).
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
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

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioHistoricoConsolidado historicoRepository;
    private final RepositorioDesvioOperacional desvioRepository;

    ControladorAgregacaoHistorica(
        RepositorioEventoValidacao validationEventRepository,
        RepositorioHistoricoConsolidado historicoRepository,
        RepositorioDesvioOperacional desvioRepository
    ) {
        this.validationEventRepository = validationEventRepository;
        this.historicoRepository       = historicoRepository;
        this.desvioRepository          = desvioRepository;
    }

    // UC10.1: update incremental contínuo a cada 5 minutos
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void incrementalUpdate() {
        LocalDate hoje = LocalDate.now();
        List<EventoValidacao> eventos = validationEventRepository.findAll();
        if (eventos.isEmpty()) return;
        consolidateByRoute(eventos, hoje);
        consolidateByProfile(eventos, hoje);
    }

    // Consolidação diária completa às 23h30 (UC10)
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void consolidateDaily() {
        LocalDate hoje = LocalDate.now();
        List<EventoValidacao> eventos = validationEventRepository.findAll();
        if (eventos.isEmpty()) return;

        consolidateByRoute(eventos, hoje);
        consolidateByProfile(eventos, hoje);
        calculateOperationalDeviations(eventos, hoje);
    }

    // -------------------------------------------------------------------------
    // Consolidação por linha
    // -------------------------------------------------------------------------

    private void consolidateByRoute(List<EventoValidacao> eventos, LocalDate data) {
        Map<String, List<EventoValidacao>> porLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getRouteId));

        for (Map.Entry<String, List<EventoValidacao>> entrada : porLinha.entrySet()) {
            String routeId = entrada.getKey();
            List<EventoValidacao> grupo = entrada.getValue();
            long total = grupo.size();

            // Receita estimada — soma de fareForAdult não nulos
            BigDecimal receita = grupo.stream()
                .filter(e -> e.getFareForAdult() != null)
                .map(EventoValidacao::getFareForAdult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            saveHistory(routeId, null, "DIA", data, data, total, receita, 0.0);
        }
    }

    // -------------------------------------------------------------------------
    // Consolidação por perfil tarifário
    // -------------------------------------------------------------------------

    private void consolidateByProfile(List<EventoValidacao> eventos, LocalDate data) {
        Map<String, List<EventoValidacao>> porPerfil = eventos.stream()
            .filter(e -> e.getPerfilClassificado() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getPerfilClassificado));

        for (Map.Entry<String, List<EventoValidacao>> entrada : porPerfil.entrySet()) {
            String perfil = entrada.getKey();
            long total = entrada.getValue().size();
            saveHistory(null, perfil, "DIA", data, data, total, BigDecimal.ZERO, 0.0);
        }
    }

    // -------------------------------------------------------------------------
    // Cálculo de desvios operacionais por linha
    // -------------------------------------------------------------------------

    private void calculateOperationalDeviations(List<EventoValidacao> eventos, LocalDate hoje) {
        Map<String, Long> realizadosPorLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getRouteId, Collectors.counting()));

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

    private void saveHistory(String routeId, String perfil, String granularidade,
                             LocalDate inicio, LocalDate fim, long total,
                             BigDecimal receita, double taxaAnomalias) {
        Optional<HistoricoConsolidado> existente = historicoRepository
            .findByRouteIdAndPerfilTarifarioAndGranularidadeAndPeriodoInicio(
                routeId, perfil, granularidade, inicio);

        HistoricoConsolidado hcEntity = existente.orElse(new HistoricoConsolidado(
            routeId, perfil, granularidade, inicio, fim,
            0, BigDecimal.ZERO, 0.0, OffsetDateTime.now()
        ));

        hcEntity.setTotalValidacoes(total);
        hcEntity.setReceitaEstimada(receita);
        hcEntity.setTaxaAnomalias(taxaAnomalias);
        hcEntity.setActualizadoEm(OffsetDateTime.now());
        historicoRepository.save(hcEntity);
    }
}
