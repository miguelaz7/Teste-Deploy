package pt.tub.ticketub.p4_apresentacao;

import pt.tub.ticketub.p4_apresentacao.ValidationDashboardMetricsDto;
import pt.tub.ticketub.p4_apresentacao.ValidationDashboardMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validations")
public class ValidationDashboardController {

    private final ValidationDashboardMetricsService validationDashboardMetricsService;

    public ValidationDashboardController(ValidationDashboardMetricsService validationDashboardMetricsService) {
        this.validationDashboardMetricsService = validationDashboardMetricsService;
    }

    @GetMapping("/dashboard-metrics")
    public ValidationDashboardMetricsDto obterMetricasDashboard() {
        return validationDashboardMetricsService.obterMetricas();
    }
}






