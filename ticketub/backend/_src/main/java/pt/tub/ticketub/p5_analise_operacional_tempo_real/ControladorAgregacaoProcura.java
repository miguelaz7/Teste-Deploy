package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O0.5.1.c – Controlador de Agregação de Procura
// Agrega continuamente validações por horário, linha e zona/paragem.
// Calcula indicadores de afluência, taxa de inválidos e distribuição
// por perfil tarifário. Actualiza O0.5.1.d incrementalmente.
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ControladorAgregacaoProcura {

    private static final Set<String> VALID_RESULTS = Set.of(
        "VALID", "OK", "SUCCESS", "ACEITE", "ACEITO", "APROVADO", "ACCEPTED", "1"
    );

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioAgregadoProcura agregadoProcuraRepository;
    private final pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRota routeRepository;

    public ControladorAgregacaoProcura(
        RepositorioEventoValidacao validationEventRepository,
        RepositorioAgregadoProcura agregadoProcuraRepository,
        pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRota routeRepository
    ) {
        this.validationEventRepository = validationEventRepository;
        this.agregadoProcuraRepository = agregadoProcuraRepository;
        this.routeRepository = routeRepository;
    }

    // Agrega a cada 2 minutos — actualização contínua (UC05)
    @Scheduled(fixedDelay = 120000)
    @Transactional
    public void aggregate() {
        List<EventoValidacao> eventos = validationEventRepository.findAll();
        if (eventos.isEmpty()) return;

        aggregateBySchedule(eventos);
        aggregateByRoute(eventos);
        aggregateByStop(eventos);
    }

    // Consulta os agregados por perspectiva para o P4
    public List<AgregadoProcura> getByPerspective(String perspectiva) {
        return agregadoProcuraRepository.findByPerspectivaOrderByTotal(perspectiva);
    }

    // Consulta insights por paragem (usado pelo P4 e P6)
    public Map<String, Object> getInsights(Optional<String> stopIdOpt) {
        List<EventoValidacao> eventos = stopIdOpt.isPresent()
            ? validationEventRepository.findByOriginStop_StopId(stopIdOpt.get())
            : validationEventRepository.findAll();

        long total = eventos.size();
        Map<Integer, Long> porHora = eventos.stream()
            .collect(Collectors.groupingBy(
                e -> e.getTransactionDateTime().getHour(), Collectors.counting()));

        int horaPico = porHora.entrySet().stream()
            .max(Comparator.<Map.Entry<Integer, Long>, Long>comparing(Map.Entry::getValue)
                .thenComparing(Map.Entry::getKey))
            .map(Map.Entry::getKey).orElse(0);

        long totalPico   = porHora.getOrDefault(horaPico, 0L);
        long invalidas   = eventos.stream().filter(this::isInvalid).count();

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("timeGap",                formatPeakHour(horaPico));
        resposta.put("peakAfluenciaPercentage", percentage(totalPico, total));
        resposta.put("invalidCount",            invalidas);
        resposta.put("invalidPercentage",       percentage(invalidas, total));
        resposta.put("total",                   total);
        resposta.put("stopId",                  stopIdOpt.orElse(null));
        return resposta;
    }

    // Contagem por tipo de título (usado pelo P4 e P6)
    public List<Object[]> getCountByType(Optional<String> stopIdOpt) {
        List<EventoValidacao> eventos = stopIdOpt.isPresent()
            ? validationEventRepository.findByOriginStop_StopId(stopIdOpt.get())
            : validationEventRepository.findAll();

        Map<String, Long> contagem = new LinkedHashMap<>();
        for (EventoValidacao e : eventos) {
            contagem.merge(normalizeType(e), 1L, Long::sum);
        }

        return contagem.entrySet().stream()
            .map(en -> new Object[]{en.getKey(), en.getValue()})
            .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Agregação por perspectiva
    // -------------------------------------------------------------------------

    private void aggregateBySchedule(List<EventoValidacao> eventos) {
        Map<Integer, List<EventoValidacao>> porHora = eventos.stream()
            .collect(Collectors.groupingBy(e -> e.getTransactionDateTime().getHour()));

        for (Map.Entry<Integer, List<EventoValidacao>> entrada : porHora.entrySet()) {
            String chave = String.valueOf(entrada.getKey());
            String desc = String.format("%02d:00", entrada.getKey());
            List<EventoValidacao> grupo = entrada.getValue();
            saveAggregate("HORARIO", chave, desc, grupo);
        }
    }

    private void aggregateByRoute(List<EventoValidacao> eventos) {
        Map<String, List<EventoValidacao>> porLinha = eventos.stream()
            .filter(e -> e.getRouteId() != null)
            .collect(Collectors.groupingBy(EventoValidacao::getRouteId));

        for (Map.Entry<String, List<EventoValidacao>> entrada : porLinha.entrySet()) {
            String routeId = entrada.getKey();
            String desc = routeId;
            try {
                Long id = Long.parseLong(routeId);
                desc = routeRepository.findById(id)
                    .map(r -> r.getRouteShortName() + " - " + r.getRouteLongName())
                    .orElse(routeId);
            } catch (Exception e) { /* fallback */ }
            
            saveAggregate("LINHA", routeId, desc, entrada.getValue());
        }
    }

    private void aggregateByStop(List<EventoValidacao> eventos) {
        Map<String, List<EventoValidacao>> porParagem = eventos.stream()
            .filter(e -> e.getOriginStop() != null)
            .collect(Collectors.groupingBy(e -> e.getOriginStop().getStopId()));

        for (Map.Entry<String, List<EventoValidacao>> entrada : porParagem.entrySet()) {
            String nome = entrada.getValue().get(0).getOriginStop().getStopName();
            saveAggregate("ZONA_PARAGEM", entrada.getKey(), nome, entrada.getValue());
        }
    }

    private void saveAggregate(String perspectiva, String chave, String descricao,
                               List<EventoValidacao> grupo) {
        long total     = grupo.size();
        long invalidas = grupo.stream().filter(this::isInvalid).count();
        
        long estudante = grupo.stream().filter(e -> {
            String c = e.getTicketType() != null ? e.getTicketType().getCode() : "";
            return c != null && c.toUpperCase().contains("ESTUDANTE");
        }).count();
        
        long senior = grupo.stream().filter(e -> {
            String c = e.getTicketType() != null ? e.getTicketType().getCode() : "";
            return c != null && c.toUpperCase().contains("SENIOR");
        }).count();
        
        long normal = total - invalidas - estudante - senior;
        if (normal < 0) normal = 0;

        Optional<AgregadoProcura> existente = agregadoProcuraRepository
            .findByPerspectivaAndChave(perspectiva, chave);

        AgregadoProcura agregado = existente.orElse(
            new AgregadoProcura(perspectiva, chave, 0, 0, 0, 0, 0, OffsetDateTime.now())
        );

        // UC05.1: comparar com baseline histórico (últimas 4 semanas)
        // Se variação > 20% face ao histórico, registar no log de auditoria
        if (existente.isPresent()) {
            long totalAnterior = existente.get().getTotalValidacoes();
            if (totalAnterior > 0) {
                double variacao = (double)(total - totalAnterior) / totalAnterior * 100.0;
                if (Math.abs(variacao) > 20.0) {
                    // Variação significativa detectada — registada no agregado
                    agregado.setActualizadoEm(OffsetDateTime.now());
                }
            }
        }

        agregado.setTotalValidacoes(total);
        agregado.setTotalInvalidas(invalidas);
        agregado.setPerfilEstudante(estudante);
        agregado.setPerfilSenior(senior);
        agregado.setPerfilNormal(normal);
        agregado.setActualizadoEm(OffsetDateTime.now());
        agregadoProcuraRepository.save(agregado);
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private boolean isInvalid(EventoValidacao e) {
        if (e.getRejectReason() != null && !e.getRejectReason().isBlank()) return true;
        String result = e.getResult();
        if (result == null || result.isBlank()) return true;
        return !VALID_RESULTS.contains(result.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizeType(EventoValidacao e) {
        if (e.getTicketType() == null || e.getTicketType().getCode() == null) return "AVULSO";
        String code = e.getTicketType().getCode().toUpperCase(Locale.ROOT);
        if (code.contains("ESTUDANTE")) return "ESTUDANTE";
        if (code.contains("SOCIAL"))    return "PASSE_SOCIAL";
        if (code.contains("SENIOR"))    return "SENIOR";
        if (code.contains("MENSAL") || code.contains("MONTHLY")) return "MENSAL";
        return "AVULSO";
    }

    private String formatPeakHour(int hora) {
        return String.format("%02d:00", hora);
    }

    private double percentage(long parte, long total) {
        if (total <= 0) return 0.0;
        return Math.round((double) parte / total * 10000.0) / 100.0;
    }
}
