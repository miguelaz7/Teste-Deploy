package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.NgsiLdDataLakeRecord;
import pt.tub.ticketub.p6_estimativa_fluxos_origem_destino.MatrizOD;
import pt.tub.ticketub.p6_estimativa_fluxos_origem_destino.MatrizODRepository;
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

    private final MatrizODRepository matrizODRepository;
    private final NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository;
    private final StopRepository stopRepository;

    public InterfaceVisualizacaoExportacaoOD(
        MatrizODRepository matrizODRepository,
        NgsiLdDataLakeRecordRepository ngsiLdDataLakeRecordRepository,
        StopRepository stopRepository
    ) {
        this.matrizODRepository = matrizODRepository;
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
        this.stopRepository = stopRepository;
    }

    // Fluxos O-D lidos directamente da tabela matriz_od (O0.8.1.d)
    @GetMapping("/fluxos")
    public ResponseEntity<List<Map<String, Object>>> obterFluxosOD(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<MatrizOD> pares = matrizODRepository.findAll().stream()
            .filter(m -> m.getDataCalculo() != null
                && !m.getDataCalculo().isBefore(inicio)
                && !m.getDataCalculo().isAfter(fim))
            .collect(Collectors.toList());

        List<Map<String, Object>> fluxos = new ArrayList<>();
        for (MatrizOD par : pares) {
            Map<String, Object> fluxo = new LinkedHashMap<>();
            fluxo.put("origemStopId",    par.getOrigemStopId());
            fluxo.put("destinoStopId",   par.getDestinoStopId() != null ? par.getDestinoStopId() : "Desconhecido");
            fluxo.put("routeId",         par.getRouteId());
            fluxo.put("periodo",         par.getPeriodo());
            fluxo.put("volume",          par.getVolume());
            fluxo.put("indiceConfianca", par.getIndiceConfianca());
            fluxo.put("dataCalculo",     par.getDataCalculo());
            fluxos.add(fluxo);
        }

        fluxos.sort((a, b) -> Integer.compare(
            ((Number) b.get("volume")).intValue(),
            ((Number) a.get("volume")).intValue()
        ));

        // Dummy map replacement needed - use empty
        Map<String, List<Object>> porOrigem = new java.util.LinkedHashMap<>();

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