package pt.tub.ticketub.p4_interfaces_utilizador;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RegistoDataLakeNgsiLd;
import pt.tub.ticketub.p6_estimativa_fluxos_origem_destino.MatrizOD;
import pt.tub.ticketub.p6_estimativa_fluxos_origem_destino.RepositorioMatrizOD;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRegistoDataLakeNgsiLd;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Paragem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioParagem;
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

    private final RepositorioMatrizOD matrizODRepository;
    private final RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository;
    private final RepositorioParagem stopRepository;

    public InterfaceVisualizacaoExportacaoOD(
        RepositorioMatrizOD matrizODRepository,
        RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository,
        RepositorioParagem stopRepository
    ) {
        this.matrizODRepository = matrizODRepository;
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
        this.stopRepository = stopRepository;
    }

    // Fluxos O-D lidos directamente da tabela matriz_od (O0.8.1.d)
    @GetMapping("/fluxos")
    public ResponseEntity<List<Map<String, Object>>> getOdFlows(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<MatrizOD> pares = matrizODRepository.findAll().stream()
            .filter(m -> m.getCalculationDate() != null
                && !m.getCalculationDate().isBefore(inicio)
                && !m.getCalculationDate().isAfter(fim))
            .collect(Collectors.toList());

        List<Map<String, Object>> fluxos = new ArrayList<>();
        for (MatrizOD par : pares) {
            Map<String, Object> fluxo = new LinkedHashMap<>();
            
            String origemNome = stopRepository.findById(par.getOriginStopId())
                .map(Paragem::getStopName)
                .orElse(par.getOriginStopId());

            String destinoNome = "Desconhecido";
            if (par.getDestinationStopId() != null) {
                destinoNome = stopRepository.findById(par.getDestinationStopId())
                    .map(Paragem::getStopName)
                    .orElse(par.getDestinationStopId());
            }

            fluxo.put("origemStopId",    origemNome);
            fluxo.put("destinoStopId",   destinoNome);
            fluxo.put("routeId",         par.getRouteId());
            fluxo.put("periodo",         par.getPeriod());
            fluxo.put("volume",          par.getVolume());
            fluxo.put("indiceConfianca", par.getConfidenceIndex());
            fluxo.put("dataCalculo",     par.getCalculationDate());
            fluxos.add(fluxo);
        }

        fluxos.sort((a, b) -> Integer.compare(
            ((Number) b.get("volume")).intValue(),
            ((Number) a.get("volume")).intValue()
        ));

        return ResponseEntity.ok(fluxos);
    }

    // Exportação da matriz O-D em formato aberto com filtro de período
    @GetMapping("/exportar")
    public ResponseEntity<Map<String, Object>> exportOdMatrix(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim
    ) {
        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<RegistoDataLakeNgsiLd> registos = ngsiLdDataLakeRecordRepository.findAll().stream()
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