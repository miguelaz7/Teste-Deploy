package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O010.1.c – Controlador de Agregação Histórica (UC10.1 / UC10.2)
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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ControladorAgregacaoHistorica {

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioHistoricoConsolidado historicoRepository;
    private final RepositorioDesvioOperacional desvioRepository;

    // Checkpoint de ponto de controlo incremental
    private OffsetDateTime lastProcessedCheckpoint = OffsetDateTime.now().minusDays(30);
    private final List<String> falhasRecuperacaoLog = new ArrayList<>();

    public ControladorAgregacaoHistorica(
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
        OffsetDateTime checkpointInicio = lastProcessedCheckpoint;
        OffsetDateTime checkpointFim = OffsetDateTime.now();
        try {
            // Recupera eventos novos desde a última atualização (timestamp de ponto de controlo)
            List<EventoValidacao> novosEventos = validationEventRepository.findAll().stream()
                .filter(e -> e.getTransactionDateTime() != null 
                    && e.getTransactionDateTime().isAfter(checkpointInicio) 
                    && !e.getTransactionDateTime().isAfter(checkpointFim))
                .collect(Collectors.toList());

            if (!novosEventos.isEmpty()) {
                // Backend identifica partição de data
                Map<LocalDate, List<EventoValidacao>> porDia = novosEventos.stream()
                    .collect(Collectors.groupingBy(e -> e.getTransactionDateTime().toLocalDate()));

                for (Map.Entry<LocalDate, List<EventoValidacao>> diaEntry : porDia.entrySet()) {
                    updateHourlyAndRouteAggregates(diaEntry.getValue(), diaEntry.getKey());
                }
            }

            // Atualiza ponto de controlo para próximo ciclo
            lastProcessedCheckpoint = checkpointFim;

        } catch (Exception e) {
            // FA1 – Falha na atualização incremental
            String errorMsg = String.format("Falha no intervalo incremental [%s a %s]: %s", checkpointInicio, checkpointFim, e.getMessage());
            falhasRecuperacaoLog.add(errorMsg);
            
            // Tenta recuperação automática do intervalo sem interromper a ingestão corrente
            try {
                autoRecoverInterval(checkpointInicio.toLocalDate(), checkpointFim.toLocalDate());
            } catch (Exception recErr) {
                falhasRecuperacaoLog.add("Recuperação automática falhou: " + recErr.getMessage());
            }
        }
    }

    // Consolidação diária completa às 23h30 (UC10)
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void consolidateDaily() {
        LocalDate hoje = LocalDate.now();
        List<EventoValidacao> eventos = validationEventRepository.findAll();
        if (eventos.isEmpty()) return;

        // Atualizar aggregates para o dia corrente
        updateHourlyAndRouteAggregates(eventos, hoje);
        calculateOperationalDeviations(eventos, hoje);
    }

    // Método público para reprocessamento manual de partição em falha (FA1 / Cenários-chave)
    @Transactional
    public void reprocessRange(LocalDate inicio, LocalDate fim) {
        // 1. Apaga os agregados históricos no intervalo de reprocessamento
        List<HistoricoConsolidado> records = historicoRepository.findAll().stream()
            .filter(h -> !h.getPeriodoInicio().isBefore(inicio) && !h.getPeriodoFim().isAfter(fim))
            .collect(Collectors.toList());
        historicoRepository.deleteAll(records);

        // 2. Busca eventos no intervalo de datas
        List<EventoValidacao> eventos = validationEventRepository.findAll().stream()
            .filter(e -> e.getTransactionDateTime() != null 
                && !e.getTransactionDateTime().toLocalDate().isBefore(inicio) 
                && !e.getTransactionDateTime().toLocalDate().isAfter(fim))
            .collect(Collectors.toList());

        if (eventos.isEmpty()) return;

        // 3. Incrementa contadores
        Map<LocalDate, List<EventoValidacao>> porDia = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getTransactionDateTime().toLocalDate()));

        for (Map.Entry<LocalDate, List<EventoValidacao>> entry : porDia.entrySet()) {
            updateHourlyAndRouteAggregates(entry.getValue(), entry.getKey());
        }
    }

    // Recuperação automática do intervalo (FA1)
    private void autoRecoverInterval(LocalDate inicio, LocalDate fim) {
        reprocessRange(inicio, fim);
    }

    // Atualização de contadores incrementais
    private void updateHourlyAndRouteAggregates(List<EventoValidacao> eventos, LocalDate dia) {
        // A. Contadores por Linha: total_validacoes, total_receita_estimada
        Map<String, List<EventoValidacao>> porLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getRouteId));

        for (Map.Entry<String, List<EventoValidacao>> ent : porLinha.entrySet()) {
            String routeId = ent.getKey();
            List<EventoValidacao> grupo = ent.getValue();
            long count = grupo.size();
            BigDecimal receita = grupo.stream()
                .filter(e -> e.getFareForAdult() != null)
                .map(EventoValidacao::getFareForAdult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            incrementHistory(routeId, null, "DIA", dia, dia, count, receita);
        }

        // B. Contadores por Perfil Tarifário (perfis_tarifarios)
        Map<String, List<EventoValidacao>> porPerfil = eventos.stream()
            .filter(e -> e.getPerfilClassificado() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getPerfilClassificado));

        for (Map.Entry<String, List<EventoValidacao>> ent : porPerfil.entrySet()) {
            String perfil = ent.getKey();
            long count = ent.getValue().size();
            incrementHistory(null, perfil, "DIA", dia, dia, count, BigDecimal.ZERO);
        }

        // C. Contadores por Horário (00h-01h, 01h-02h, 23h-24h)
        Map<Integer, List<EventoValidacao>> porHora = eventos.stream()
            .filter(e -> e.getTransactionDateTime() != null)
            .collect(Collectors.groupingBy(e -> e.getTransactionDateTime().getHour()));

        for (Map.Entry<Integer, List<EventoValidacao>> ent : porHora.entrySet()) {
            int hour = ent.getKey();
            String granularidadeHora = String.format("HORA_%02d", hour);
            long count = ent.getValue().size();
            BigDecimal receita = ent.getValue().stream()
                .filter(e -> e.getFareForAdult() != null)
                .map(EventoValidacao::getFareForAdult)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            incrementHistory(null, null, granularidadeHora, dia, dia, count, receita);
        }
    }

    // Incrementa contadores no histórico existente
    private void incrementHistory(String routeId, String perfil, String granularidade,
                                  LocalDate inicio, LocalDate fim, long count, BigDecimal receita) {
        Optional<HistoricoConsolidado> existente = historicoRepository
            .findByRouteIdAndPerfilTarifarioAndGranularidadeAndPeriodoInicio(
                routeId, perfil, granularidade, inicio);

        HistoricoConsolidado hcEntity = existente.orElse(new HistoricoConsolidado(
            routeId, perfil, granularidade, inicio, fim,
            0, BigDecimal.ZERO, 0.0, OffsetDateTime.now()
        ));

        hcEntity.setTotalValidacoes(hcEntity.getTotalValidacoes() + count);
        if (receita != null) {
            hcEntity.setReceitaEstimada(hcEntity.getReceitaEstimada().add(receita));
        }
        hcEntity.setActualizadoEm(OffsetDateTime.now());
        historicoRepository.save(hcEntity);
    }

    // Cálculo de desvios operacionais por linha
    private void calculateOperationalDeviations(List<EventoValidacao> eventos, LocalDate hoje) {
        Map<String, Long> realizadosPorLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getRouteId, Collectors.counting()));

        for (Map.Entry<String, Long> entrada : realizadosPorLinha.entrySet()) {
            String routeId = entrada.getKey();
            long realizados = entrada.getValue();

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

    public List<String> getFalhasRecuperacaoLog() {
        return falhasRecuperacaoLog;
    }
}
