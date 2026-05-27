package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// =============================================================================
// O0.9.1.c – Controlador de Deteção de Anomalias (UC09.1 & UC09.3)
// Aplica regras parametrizáveis a cada evento: título expirado, duplicação,
// título inválido, evasão tarifária, desvio bilhete/linha e sobrelotação.
// Classifica severidade, notifica a equipa e escala incidentes.
// Consome dados de: O0.9.1.d (AlertaRepository).
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ControladorDetecaoAnomalias {

    // Limiares de volume para quarentena
    private static final long LIMIAR_CRITICO = 50;
    private static final long LIMIAR_AVISO   = 10;

    private final RepositorioAlerta alertaRepository;
    private final RepositorioQuarentenaValidacao quarantineRepository;
    private final RepositorioEventoValidacao validationEventRepository;

    public ControladorDetecaoAnomalias(
        RepositorioAlerta alertaRepository,
        RepositorioQuarentenaValidacao quarantineRepository,
        RepositorioEventoValidacao validationEventRepository
    ) {
        this.alertaRepository         = alertaRepository;
        this.quarantineRepository     = quarantineRepository;
        this.validationEventRepository = validationEventRepository;
    }

    // Corre a cada 5 minutos para analisar anomalias recentes
    @Scheduled(fixedDelay = 300000)
    @Transactional
    public void analyzeAnomalies() {
        OffsetDateTime janela5m = OffsetDateTime.now().minusMinutes(5);
        OffsetDateTime janela24h = OffsetDateTime.now().minusHours(24);

        // Buscar eventos das últimas 24 horas para análise de padrões
        List<EventoValidacao> eventos = validationEventRepository.findAll().stream()
            .filter(e -> e.getTransactionDateTime() != null && e.getTransactionDateTime().isAfter(janela24h))
            .collect(Collectors.toList());

        // ---------------------------------------------------------------------
        // Regra 1: "Múltiplas validações na mesma identidade em tempo curto" (ex: 3 validações em <5 min)
        // ---------------------------------------------------------------------
        Map<String, List<EventoValidacao>> porCard = eventos.stream()
            .filter(e -> e.getCardId() != null && !e.getCardId().isBlank())
            .collect(Collectors.groupingBy(EventoValidacao::getCardId));

        for (Map.Entry<String, List<EventoValidacao>> entry : porCard.entrySet()) {
            List<EventoValidacao> cardEvents = new ArrayList<>(entry.getValue());
            cardEvents.sort(Comparator.comparing(EventoValidacao::getTransactionDateTime));

            for (int i = 0; i < cardEvents.size(); i++) {
                int count = 1;
                OffsetDateTime baseTime = cardEvents.get(i).getTransactionDateTime();
                for (int j = i + 1; j < cardEvents.size(); j++) {
                    long diffSeconds = java.time.Duration.between(baseTime, cardEvents.get(j).getTransactionDateTime()).abs().toSeconds();
                    if (diffSeconds < 300) { // < 5 min
                        count++;
                    } else {
                        break;
                    }
                }
                if (count >= 3) {
                    createAlert("MULTIPLE_VALIDATIONS", "AVISO", null,
                        "Evasão suspeita: Cartão " + entry.getKey().substring(0, Math.min(8, entry.getKey().length())) 
                        + "... validado " + count + " vezes em menos de 5 minutos.");
                    break;
                }
            }
        }

        // ---------------------------------------------------------------------
        // Regra 2: "Desvio de bilhete para linha" (tipo_bilhete "Escolar" mas linha "Noturna")
        // ---------------------------------------------------------------------
        for (EventoValidacao ev : eventos) {
            if (ev.getTicketType() != null && ev.getTicketType().getCode() != null) {
                String tCode = ev.getTicketType().getCode().toLowerCase();
                String route = ev.getRouteId() != null ? ev.getRouteId().toLowerCase() : "";
                int hour = ev.getTransactionDateTime().getHour();

                boolean isEscolar = tCode.contains("escolar") || tCode.contains("student");
                boolean isNightLine = route.contains("noturna") || route.startsWith("n") || hour >= 23 || hour < 6;

                if (isEscolar && isNightLine) {
                    createAlert("DESVIO_LINHA", "AVISO", ev.getIngestionHash(),
                        "Falha de controlo: Bilhete Escolar usado na linha/horário " + ev.getRouteId() + " às " + hour + "h.");
                }
            }
        }

        // ---------------------------------------------------------------------
        // Regra 3: "Discrepância ocupação vs. validações" (mais validações que lugares disponíveis)
        // ---------------------------------------------------------------------
        Map<String, List<EventoValidacao>> porTrip = eventos.stream()
            .filter(e -> e.getTripId() != null && !e.getTripId().isBlank())
            .collect(Collectors.groupingBy(EventoValidacao::getTripId));

        for (Map.Entry<String, List<EventoValidacao>> entry : porTrip.entrySet()) {
            List<EventoValidacao> tripEvents = entry.getValue();
            int totalVal = tripEvents.size();
            // Assumimos limite de lotação de 80 validações num único tripId
            if (totalVal > 80) {
                createAlert("EXCESSO_VALIDACOES", "CRITICO", null,
                    "Discrepância ocupação: " + totalVal + " validações no tripId " + entry.getKey() + " (lotação excedida).");
            }
        }

        // ---------------------------------------------------------------------
        // Regra 4: "Padrão recorrente" (mesma identidade, mesma linha, mesma hora diariamente com bilhete diário)
        // ---------------------------------------------------------------------
        Map<String, List<EventoValidacao>> porCardRoute = eventos.stream()
            .filter(e -> e.getCardId() != null && !e.getCardId().isBlank() && e.getRouteId() != null)
            .collect(Collectors.groupingBy(e -> e.getCardId() + "|" + e.getRouteId()));

        for (Map.Entry<String, List<EventoValidacao>> entry : porCardRoute.entrySet()) {
            List<EventoValidacao> list = entry.getValue();
            if (list.size() >= 3) {
                boolean suspicious = false;
                for (int i = 0; i < list.size(); i++) {
                    EventoValidacao e1 = list.get(i);
                    String tCode = e1.getTicketType() != null ? e1.getTicketType().getCode().toLowerCase() : "";
                    
                    // Fraude se usar bilhete diário/simples diariamente à mesma hora
                    if (tCode.contains("diario") || tCode.contains("simples") || tCode.contains("single") || tCode.contains("1 viagem")) {
                        int h1 = e1.getTransactionDateTime().getHour();
                        int matchCount = 1;
                        for (int j = i + 1; j < list.size(); j++) {
                            EventoValidacao e2 = list.get(j);
                            int h2 = e2.getTransactionDateTime().getHour();
                            long daysDiff = java.time.temporal.ChronoUnit.DAYS.between(
                                e1.getTransactionDateTime().toLocalDate(), 
                                e2.getTransactionDateTime().toLocalDate()
                            );
                            if (daysDiff > 0 && Math.abs(h1 - h2) <= 1) {
                                matchCount++;
                            }
                        }
                        if (matchCount >= 3) {
                            suspicious = true;
                            break;
                        }
                    }
                }
                if (suspicious) {
                    String[] parts = entry.getKey().split("\\|");
                    String card = parts[0];
                    String rId = parts[1];
                    createAlert("PADRAO_FRAUDE", "AVISO", null,
                        "Suspeita de fraude: Padrão recorrente detetado para cartão " + card.substring(0, Math.min(8, card.length())) 
                        + " na rota " + rId + " em dias consecutivos.");
                }
            }
        }

        // Regra Ingestão: volume de registos em quarentena
        long totalQuarentena = quarantineRepository.countByCreatedAtAfter(janela5m);
        if (totalQuarentena >= LIMIAR_CRITICO) {
            createAlert("TITULO_INVALIDO", "CRITICO", null,
                "Volume crítico de rejeições: " + totalQuarentena + " nos últimos 5 minutos");
        } else if (totalQuarentena >= LIMIAR_AVISO) {
            createAlert("TITULO_INVALIDO", "AVISO", null,
                "Volume elevado de rejeições: " + totalQuarentena + " nos últimos 5 minutos");
        }

        // Aplicar as regras de escalação e status de severidade (UC09.3)
        applyEscalationAndStatus();
    }

    // Analisa um evento específico recém-ingerido
    @Transactional
    public void analyzeEvent(EventoValidacao evento) {
        if (evento == null) return;

        if ("REJECTED".equalsIgnoreCase(evento.getResult())
                || "INVALIDO".equalsIgnoreCase(evento.getResult())) {
            String severidade = classifySeverity(evento);
            createAlert("TITULO_INVALIDO", severidade,
                evento.getIngestionHash(),
                "Evento rejeitado: " + evento.getRejectReason());
        }
    }

    // Analisa registos de quarentena recém-criados
    @Transactional
    public void analyzeQuarantine(QuarentenaValidacao quarentena) {
        if (quarentena == null) return;

        String tipo = classifyQuarantineType(quarentena.getReason());
        String severidade = "AVISO";

        createAlert(tipo, severidade, null,
            "Registo em quarentena: " + quarentena.getReason());
    }

    // UC09.3: Classificar severidade e escalonar
    @Transactional
    public void applyEscalationAndStatus() {
        List<Alerta> ativos = alertaRepository.findAll().stream()
            .filter(a -> "PENDENTE".equals(a.getStatus()) || "EM_ANALISE".equals(a.getStatus()) || "EM_ESCALACAO".equals(a.getStatus()))
            .collect(Collectors.toList());

        for (Alerta a : ativos) {
            String routeId = extractRouteIdFromAlert(a);

            // 1. Se linha tem "zona de alto risco", escalar severidade de AVISO para CRITICO
            if ("13".equals(routeId) && "AVISO".equals(a.getSeverity())) {
                a.setSeverity("CRITICO");
                alertaRepository.save(a);
            }

            // 2. Se volume de alertas na mesma linha ultrapassa 10 em 1h: escalar para CRÍTICA
            if (routeId != null) {
                long count1h = ativos.stream()
                    .filter(alt -> routeId.equals(extractRouteIdFromAlert(alt)))
                    .filter(alt -> alt.getCreatedAt().isAfter(OffsetDateTime.now().minusHours(1)))
                    .count();
                if (count1h > 10 && !"CRITICO".equals(a.getSeverity())) {
                    a.setSeverity("CRITICO");
                    alertaRepository.save(a);
                }
            }

            // 3. Se severidade = CRÍTICA: enviar para "fila de escalação" (estado EM_ESCALACAO)
            if ("CRITICO".equals(a.getSeverity()) && "PENDENTE".equals(a.getStatus())) {
                a.setStatus("EM_ESCALACAO");
                alertaRepository.save(a);
            }
        }
    }

    // Helper para extrair Route ID
    private String extractRouteIdFromAlert(Alerta a) {
        if (a.getEventData() == null) return null;
        String data = a.getEventData().toLowerCase();
        if (data.contains("tripid 13") || data.contains("linha 13") || data.contains("rota 13")) {
            return "13";
        }
        if (data.contains("tripid 2") || data.contains("linha 2") || data.contains("rota 2")) {
            return "2";
        }
        return null;
    }

    private void createAlert(String tipo, String severidade,
                             String ingestionHash, String dadosEvento) {
        // Evitar duplicar alertas ativos do mesmo tipo e mensagem nas últimas 24h
        List<Alerta> existentes = alertaRepository.findByTypeAndStatus(tipo, "PENDENTE");
        for (Alerta e : existentes) {
            if (Objects.equals(e.getEventData(), dadosEvento)) {
                return;
            }
        }
        alertaRepository.save(new Alerta(
            tipo, severidade, ingestionHash, dadosEvento, OffsetDateTime.now()
        ));
    }

    private String classifySeverity(EventoValidacao evento) {
        if (evento.getRejectReason() != null
                && evento.getRejectReason().toLowerCase().contains("expirado")) {
            return "AVISO";
        }
        return "CRITICO";
    }

    private String classifyQuarantineType(String reason) {
        if (reason == null) return "TITULO_INVALIDO";
        String r = reason.toLowerCase();
        if (r.contains("duplicado")) return "DUPLICADO";
        if (r.contains("expirado")) return "TITULO_EXPIRADO";
        return "TITULO_INVALIDO";
    }
}
