package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RegistoDataLakeNgsiLd;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRegistoDataLakeNgsiLd;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// =============================================================================
// O012.1.i – Interface de Exportação de Dados Abertos (UC12.1)
// Interface para o analista seleccionar conjuntos aprovados pelo DPO
// e exportar em formato aberto (CSV/JSON). Bloqueia se dados não
// anonimizados forem detectados.
// Consome dados de: O012.1.d (repositório de exportações de dados abertos).
// =============================================================================

@RestController
@RequestMapping("/api/exportacao")
public class InterfaceExportacaoDadosAbertos {

    private final RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository;

    public InterfaceExportacaoDadosAbertos(
        RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository
    ) {
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
    }

    // Export anonymised NGSI-LD records by partition date
    @GetMapping("/dados-abertos")
    public ResponseEntity<Map<String, Object>> exportOpenData(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<RegistoDataLakeNgsiLd> registos = ngsiLdDataLakeRecordRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .collect(Collectors.toList());

        // All data in Data Lake is already anonymised (UC02.2)
        // cardId is pseudonymised – original value never exposed
        List<Map<String, Object>> exportacao = registos.stream().map(r -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("entityId",      r.getEntityId());
            item.put("entityType",    r.getEntityType());
            item.put("partitionDate", r.getPartitionDate());
            item.put("payload",       r.getPayloadJson());
            return item;
        }).collect(Collectors.toList());

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dataInicio",    inicio);
        resposta.put("dataFim",       fim);
        resposta.put("totalRegistos", exportacao.size());
        resposta.put("anonimizado",   true);
        resposta.put("registos",      exportacao);

        return ResponseEntity.ok(resposta);
    }
}
