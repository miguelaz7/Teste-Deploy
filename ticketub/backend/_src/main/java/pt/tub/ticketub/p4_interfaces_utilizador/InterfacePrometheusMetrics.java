package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEstatisticasLoteIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InterfacePrometheusMetrics {

    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioQuarentenaValidacao validationQuarantineRepository;
    private final RepositorioEstatisticasLoteIngestao statsRepository;

    public InterfacePrometheusMetrics(
        RepositorioEventoValidacao validationEventRepository,
        RepositorioQuarentenaValidacao validationQuarantineRepository,
        RepositorioEstatisticasLoteIngestao statsRepository
    ) {
        this.validationEventRepository = validationEventRepository;
        this.validationQuarantineRepository = validationQuarantineRepository;
        this.statsRepository = statsRepository;
    }

    @GetMapping(value = "/api/metrics", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getMetrics() {
        long validas = validationEventRepository.count();
        long quarentena = validationQuarantineRepository.count();
        
        long duplicados = statsRepository.findAll().stream()
            .mapToLong(s -> s.getTotalDuplicados())
            .sum();
            
        long totalRecebidos = statsRepository.findAll().stream()
            .mapToLong(s -> s.getTotalRecebidos())
            .sum();

        long lastProcessingTimeMs = statsRepository.findAll().stream()
            .reduce((first, second) -> second) // Obter o último elemento
            .map(s -> s.getTempoProcessamentoMs())
            .orElse(0L);

        StringBuilder sb = new StringBuilder();
        
        sb.append("# HELP ticketub_ingestao_recebidos_total Total de registos de validacao recebidos\n");
        sb.append("# TYPE ticketub_ingestao_recebidos_total counter\n");
        sb.append("ticketub_ingestao_recebidos_total ").append(totalRecebidos).append("\n\n");

        sb.append("# HELP ticketub_ingestao_validos_total Total de registos de validacao validos\n");
        sb.append("# TYPE ticketub_ingestao_validos_total counter\n");
        sb.append("ticketub_ingestao_validos_total ").append(validas).append("\n\n");

        sb.append("# HELP ticketub_ingestao_quarentena_total Total de registos de validacao enviados para quarentena\n");
        sb.append("# TYPE ticketub_ingestao_quarentena_total counter\n");
        sb.append("ticketub_ingestao_quarentena_total ").append(quarentena).append("\n\n");

        sb.append("# HELP ticketub_ingestao_duplicados_total Total de registos de validacao duplicados detetados\n");
        sb.append("# TYPE ticketub_ingestao_duplicados_total counter\n");
        sb.append("ticketub_ingestao_duplicados_total ").append(duplicados).append("\n\n");

        sb.append("# HELP ticketub_ingestao_tempo_ultimo_ciclo_ms Tempo de processamento do ultimo ciclo em milissegundos\n");
        sb.append("# TYPE ticketub_ingestao_tempo_ultimo_ciclo_ms gauge\n");
        sb.append("ticketub_ingestao_tempo_ultimo_ciclo_ms ").append(lastProcessingTimeMs).append("\n");

        return ResponseEntity.ok(sb.toString());
    }
}
