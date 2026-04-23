package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p4_interfaces_utilizador.ValidationDashboardMetricsDto;
import pt.tub.ticketub.p2_ingestao_processamento_dados.IngestionAuditLogRepository;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantineRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ValidationDashboardMetricsService {

    private static final List<String> EVENTOS_DUPLICADOS = List.of("DUPLICATE_INTRA_PACKAGE", "DUPLICATE_INTER_PACKAGE");

    private final ValidationEventRepository validationEventRepository;
    private final ValidationQuarantineRepository validationQuarantineRepository;
    private final IngestionAuditLogRepository ingestionAuditLogRepository;

    public ValidationDashboardMetricsService(
        ValidationEventRepository validationEventRepository,
        ValidationQuarantineRepository validationQuarantineRepository,
        IngestionAuditLogRepository ingestionAuditLogRepository
    ) {
        this.validationEventRepository = validationEventRepository;
        this.validationQuarantineRepository = validationQuarantineRepository;
        this.ingestionAuditLogRepository = ingestionAuditLogRepository;
    }

    public ValidationDashboardMetricsDto obterMetricas() {
        OffsetDateTime agora = OffsetDateTime.now();

        ValidationDashboardMetricsDto.PeriodMetrics ultimaHora = calcularPeriodo("ultima_hora", agora.minus(Duration.ofHours(1)));
        ValidationDashboardMetricsDto.PeriodMetrics ultimas24Horas = calcularPeriodo("ultimas_24_horas", agora.minus(Duration.ofHours(24)));
        ValidationDashboardMetricsDto.PeriodMetrics ultimos7Dias = calcularPeriodo("ultimos_7_dias", agora.minus(Duration.ofDays(7)));

        return new ValidationDashboardMetricsDto(ultimaHora, ultimas24Horas, ultimos7Dias);
    }

    private ValidationDashboardMetricsDto.PeriodMetrics calcularPeriodo(String periodo, OffsetDateTime inicioJanela) {
        long validas = validationEventRepository.countByIngestedAtAfter(inicioJanela);
        long quarentena = validationQuarantineRepository.countByCreatedAtAfter(inicioJanela);
        long duplicados = ingestionAuditLogRepository.countByEventTypeInAndCreatedAtAfter(EVENTOS_DUPLICADOS, inicioJanela);

        long volumeIngestao = validas + quarentena + duplicados;
        long totalQualidade = validas + quarentena;

        double taxaValidas = percentage(validas, totalQualidade);
        double taxaQuarentena = percentage(quarentena, totalQualidade);

        return new ValidationDashboardMetricsDto.PeriodMetrics(
            periodo,
            volumeIngestao,
            validas,
            quarentena,
            duplicados,
            taxaValidas,
            taxaQuarentena
        );
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






