package com.example.demo.service;

import com.example.demo.dto.StopLiveDataDto;
import com.example.demo.model.Stop;
import com.example.demo.model.ValidationEvent;
import com.example.demo.repository.StopRepository;
import com.example.demo.repository.ValidationEventRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StopLiveDataService {

    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> VALID_RESULTS = Set.of(
        "VALID",
        "OK",
        "SUCCESS",
        "ACEITE",
        "ACEITO",
        "APROVADO",
        "1"
    );

    private final StopRepository stopRepository;
    private final ValidationEventRepository validationEventRepository;

    public StopLiveDataService(StopRepository stopRepository, ValidationEventRepository validationEventRepository) {
        this.stopRepository = stopRepository;
        this.validationEventRepository = validationEventRepository;
    }

    public Optional<StopLiveDataDto> obterLiveDataPorParagem(String stopId) {
        Optional<Stop> stopOpt = stopRepository.findById(stopId);
        if (stopOpt.isEmpty()) {
            return Optional.empty();
        }

        Stop stop = stopOpt.get();
        List<ValidationEvent> eventos = validationEventRepository.findByOriginStop_StopId(stopId);
        long total = eventos.size();

        Map<String, Long> distribuicaoTitulos = inicializarDistribuicaoTitulos();
        for (ValidationEvent evento : eventos) {
            String categoria = normalizarCategoriaTitulo(evento);
            distribuicaoTitulos.compute(categoria, (k, v) -> v == null ? 1L : v + 1L);
        }

        long invalidas = eventos.stream().filter(this::isInvalidValidation).count();

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
        double percentagemHoraPico = percentage(totalHoraPico, total);
        double percentagemInvalidas = percentage(invalidas, total);

        String[] janelaPico = buildHourWindow(horaPico);

        StopLiveDataDto dto = new StopLiveDataDto(
            stop.getStopId(),
            stop.getStopName(),
            janelaPico[0],
            janelaPico[1],
            totalHoraPico,
            percentagemHoraPico,
            invalidas,
            percentagemInvalidas,
            distribuicaoTitulos,
            total
        );

        return Optional.of(dto);
    }

    private Map<String, Long> inicializarDistribuicaoTitulos() {
        Map<String, Long> distribuicao = new LinkedHashMap<>();
        distribuicao.put("Estudante", 0L);
        distribuicao.put("Mensal", 0L);
        distribuicao.put("Avulso", 0L);
        distribuicao.put("Passe Social", 0L);
        distribuicao.put("Senior", 0L);
        return distribuicao;
    }

    private String normalizarCategoriaTitulo(ValidationEvent evento) {
        if (evento.getTicketType() == null || evento.getTicketType().getCode() == null) {
            return "Avulso";
        }

        String code = evento.getTicketType().getCode().toUpperCase(Locale.ROOT);
        if (code.contains("ESTUDANTE")) {
            return "Estudante";
        }
        if (code.contains("SOCIAL")) {
            return "Passe Social";
        }
        if (code.contains("SENIOR")) {
            return "Senior";
        }
        if (code.contains("MENSAL") || code.contains("MONTHLY")) {
            return "Mensal";
        }
        return "Avulso";
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

    private String[] buildHourWindow(int hour) {
        LocalTime inicio = LocalTime.of(hour, 0);
        LocalTime fim = LocalTime.of((hour + 1) % 24, 0);
        return new String[] {
            HOUR_FORMATTER.format(inicio),
            HOUR_FORMATTER.format(fim)
        };
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
