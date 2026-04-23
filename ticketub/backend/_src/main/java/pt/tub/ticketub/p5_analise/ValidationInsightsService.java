package pt.tub.ticketub.p5_analise;

import pt.tub.ticketub.p2_ingestao.ValidationEvent;
import pt.tub.ticketub.p2_ingestao.ValidationEventRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ValidationInsightsService {

    private static final Set<String> VALID_RESULTS = Set.of(
        "VALID",
        "OK",
        "SUCCESS",
        "ACEITE",
        "ACEITO",
        "APROVADO",
        "1"
    );

    private final ValidationEventRepository validationEventRepository;

    public ValidationInsightsService(ValidationEventRepository validationEventRepository) {
        this.validationEventRepository = validationEventRepository;
    }

    public Map<String, Object> obterInsights(Optional<String> stopIdOpt) {
        List<ValidationEvent> eventos = carregarEventos(stopIdOpt);
        long total = eventos.size();

        Map<Integer, Long> contagemPorHora = eventos.stream()
            .collect(Collectors.groupingBy(
                e -> e.getTransactionDateTime().getHour(),
                Collectors.counting()
            ));

        int horaPico = contagemPorHora.entrySet().stream()
            .max(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue)
                .thenComparingInt(Map.Entry::getKey))
            .map(Map.Entry::getKey)
            .orElse(0);

        long totalHoraPico = contagemPorHora.getOrDefault(horaPico, 0L);
        long invalidas = eventos.stream().filter(this::isInvalidValidation).count();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timeGap", formatPeakHour(horaPico));
        response.put("peakAfluenciaPercentage", percentage(totalHoraPico, total));
        response.put("invalidCount", invalidas);
        response.put("invalidPercentage", percentage(invalidas, total));
        response.put("total", total);
        response.put("stopId", stopIdOpt.orElse(null));
        return response;
    }

    public List<Object[]> obterContagemPorTipo(Optional<String> stopIdOpt) {
        List<ValidationEvent> eventos = carregarEventos(stopIdOpt);

        Map<String, Long> contagem = new LinkedHashMap<>();
        for (ValidationEvent evento : eventos) {
            String tipo = normalizarTipo(evento);
            contagem.compute(tipo, (k, v) -> v == null ? 1L : v + 1L);
        }

        List<Object[]> rows = new ArrayList<>();
        for (Map.Entry<String, Long> entry : contagem.entrySet()) {
            rows.add(new Object[] {entry.getKey(), entry.getValue()});
        }
        return rows;
    }

    private List<ValidationEvent> carregarEventos(Optional<String> stopIdOpt) {
        if (stopIdOpt.isPresent() && !stopIdOpt.get().isBlank()) {
            return validationEventRepository.findByOriginStop_StopId(stopIdOpt.get());
        }
        return validationEventRepository.findAll();
    }

    private boolean isInvalidValidation(ValidationEvent evento) {
        if (evento.getRejectReason() != null && !evento.getRejectReason().isBlank()) {
            return true;
        }

        String result = evento.getResult();
        if (result == null || result.isBlank()) {
            return true;
        }

        return !VALID_RESULTS.contains(result.trim().toUpperCase(Locale.ROOT));
    }

    private String normalizarTipo(ValidationEvent evento) {
        if (evento.getTicketType() == null || evento.getTicketType().getCode() == null) {
            return "AVULSO";
        }

        String code = evento.getTicketType().getCode().toUpperCase(Locale.ROOT);
        if (code.contains("ESTUDANTE")) {
            return "ESTUDANTE";
        }
        if (code.contains("SOCIAL")) {
            return "PASSE_SOCIAL";
        }
        if (code.contains("SENIOR")) {
            return "SENIOR";
        }
        if (code.contains("MENSAL") || code.contains("MONTHLY")) {
            return "MENSAL";
        }
        return "AVULSO";
    }

    private String formatPeakHour(int hour) {
        LocalTime time = LocalTime.of(hour, 0);
        return OffsetDateTime.of(LocalDate.now(), time, ZoneOffset.UTC).toString();
    }

    private double percentage(long part, long total) {
        if (total <= 0) {
            return 0.0;
        }
        BigDecimal value = BigDecimal.valueOf(part)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
        return value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}






