package pt.tub.ticketub.p8_alertas_e_excecoes;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api/alertas")
@Validated
public class AlertMonitoringController {

    private final AlertMonitoringService alertMonitoringService;

    public AlertMonitoringController(AlertMonitoringService alertMonitoringService) {
        this.alertMonitoringService = alertMonitoringService;
    }

    @PostMapping("/deteccao/executar")
    public AlertDetectionResponseDto executarDeteccao() {
        return alertMonitoringService.runDetection();
    }

    @GetMapping
    public List<AlertIncidentResponseDto> listar(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String severity,
        @RequestParam(required = false) String category
    ) {
        return alertMonitoringService.listAlerts(
            Optional.ofNullable(parseEnum(status, AlertStatus.class)),
            Optional.ofNullable(parseEnum(severity, AlertSeverity.class)),
            Optional.ofNullable(parseEnum(category, AlertCategory.class))
        );
    }

    @GetMapping("/{id}")
    public AlertIncidentResponseDto obter(@PathVariable Long id) {
        return alertMonitoringService.getAlert(id);
    }

    @PatchMapping("/{id}/estado")
    public AlertIncidentResponseDto atualizarEstado(@PathVariable Long id, @Valid @RequestBody AlertStatusUpdateRequestDto request) {
        return alertMonitoringService.updateAlertStatus(id, request);
    }

    @PostMapping("/{id}/acoes")
    public AlertIncidentResponseDto registarAcao(@PathVariable Long id, @Valid @RequestBody AlertActionRequestDto request) {
        return alertMonitoringService.registerAction(id, request);
    }

    @GetMapping("/configuracao")
    public AlertConfigurationResponseDto obterConfiguracao() {
        return alertMonitoringService.getConfiguration();
    }

    @PutMapping("/configuracao")
    public AlertConfigurationResponseDto atualizarConfiguracao(@Valid @RequestBody AlertConfigurationRequestDto request) {
        return alertMonitoringService.updateConfiguration(request);
    }

    @GetMapping("/resumo")
    public AlertSummaryResponseDto resumo() {
        return alertMonitoringService.getSummary();
    }

    private <E extends Enum<E>> E parseEnum(String value, Class<E> enumClass) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AlertValidationException("Valor inválido para " + enumClass.getSimpleName() + ": " + value);
        }
    }
}