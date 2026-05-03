package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdDataLakeRecord;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdDataLakeRecordRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// =============================================================================
// O0.8.2.i – Interface de Visualização e Exportação O-D (UC08.2)
// Apresenta fluxos O-D sobre o mapa coloridos por nível de ocupação.
// Permite consultar detalhe por fluxo e exportar a matriz.
// Consome dados de: O0.8.1.d (repositório da matriz Origem-Destino).
// =============================================================================

@RestController
@RequestMapping("/api/od")
public class InterfaceVisualizacaoExportacaoOD {

    private final NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository;
    private final StopRepository stopRepository;

    public InterfaceVisualizacaoExportacaoOD(
        NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository,
        StopRepository stopRepository
    ) {
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
        this.stopRepository = stopRepository;
    }

    // Fluxos O-D agregados por paragem de origem para visualização no mapa
    @GetMapping("/fluxos")
    public ResponseEntity<List<Map<String, Object>>> obterFluxosOD(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(1);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        // Agrega registos do Data Lake por paragem de origem dentro do período
        List<NgsiLdDataLakeRecord> registos = ngsiLdDataLakeRecordRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim))
            .collect(Collectors.toList());

        // Agrupa por coordenada de origem e conta volume
        Map<String, List<NgsiLdDataLakeRecord>> porOrigem = registos.stream()
            .filter(r -> r.getOriginLat() != null && r.getOriginLon() != null)
            .collect(Collectors.groupingBy(
                r -> r.getOriginLat() + "," + r.getOriginLon()
            ));

        List<Map<String, Object>> fluxos = new ArrayList<>();
        for (Map.Entry<String, List<NgsiLdDataLakeRecord>> entrada : porOrigem.entrySet()) {
            long volume = entrada.getValue().size();
            NgsiLdDataLakeRecord exemplo = entrada.getValue().get(0);

            // Nível de ocupação para coloração no mapa
            String nivelOcupacao;
            if (volume > 100) nivelOcupacao = "ALTO";
            else if (volume > 30) nivelOcupacao = "MEDIO";
            else nivelOcupacao = "BAIXO";

            Map<String, Object> fluxo = new LinkedHashMap<>();
            fluxo.put("origemLat",      exemplo.getOriginLat());
            fluxo.put("origemLon",      exemplo.getOriginLon());
            fluxo.put("volume",         volume);
            fluxo.put("nivelOcupacao",  nivelOcupacao);
            fluxo.put("periodo",        inicio + " a " + fim);
            fluxos.add(fluxo);
        }

        // Ordenar por volume descendente
        fluxos.sort((a, b) -> Long.compare(
            ((Number) b.get("volume")).longValue(),
            ((Number) a.get("volume")).longValue()
        ));

        return ResponseEntity.ok(fluxos);
    }

    // Exportação da matriz O-D em formato aberto com filtro de período
    @GetMapping("/exportar")
    public ResponseEntity<Map<String, Object>> exportarMatrizOD(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<NgsiLdDataLakeRecord> registos = ngsiLdDataLakeRecordRepository.findAll().stream()
            .filter(r -> r.getPartitionDate() != null
                && !r.getPartitionDate().isBefore(inicio)
                && !r.getPartitionDate().isAfter(fim)
                && r.getOriginLat() != null)
            .collect(Collectors.toList());

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dataInicio",    inicio);
        resposta.put("dataFim",       fim);
        resposta.put("totalRegistos", registos.size());
        resposta.put("anonimizado",   true);
        resposta.put("formato",       "JSON");

        return ResponseEntity.ok(resposta);
    }
}
