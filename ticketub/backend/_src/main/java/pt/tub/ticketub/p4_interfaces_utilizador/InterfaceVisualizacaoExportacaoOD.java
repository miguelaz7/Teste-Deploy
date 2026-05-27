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

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;
import org.springframework.web.bind.annotation.RequestHeader;
import java.time.OffsetDateTime;

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
    private final RepositorioAuditoriaIngestao auditRepository;

    public InterfaceVisualizacaoExportacaoOD(
        RepositorioMatrizOD matrizODRepository,
        RepositorioRegistoDataLakeNgsiLd ngsiLdDataLakeRecordRepository,
        RepositorioParagem stopRepository,
        RepositorioAuditoriaIngestao auditRepository
    ) {
        this.matrizODRepository = matrizODRepository;
        this.ngsiLdDataLakeRecordRepository = ngsiLdDataLakeRecordRepository;
        this.stopRepository = stopRepository;
        this.auditRepository = auditRepository;
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

    // Exportação da matriz O-D em formato aberto com filtro de período (UC08.3)
    @GetMapping("/exportar")
    public ResponseEntity<?> exportOdMatrix(
        @RequestParam(required = false) String dataInicio,
        @RequestParam(required = false) String dataFim,
        @RequestParam(required = false, defaultValue = "CSV") String formato,
        @RequestHeader(value = "X-Api-User", defaultValue = "analista") String user
    ) {
        // UC08.3: Sistema valida se utilizador tem permissão de export (contraída com DPO)
        if (user == null || (!user.equals("analista") && !user.equals("admin") && !user.equals("dpo") && !user.equals("gestor"))) {
            return ResponseEntity.status(403).body(Map.of("erro", "Acesso negado: utilizador sem permissão contratada com o DPO para exportar a matriz O-D."));
        }

        LocalDate inicio = dataInicio != null ? LocalDate.parse(dataInicio) : LocalDate.now().minusDays(7);
        LocalDate fim    = dataFim    != null ? LocalDate.parse(dataFim)    : LocalDate.now();

        List<MatrizOD> pares = matrizODRepository.findAll().stream()
            .filter(m -> m.getCalculationDate() != null
                && !m.getCalculationDate().isBefore(inicio)
                && !m.getCalculationDate().isAfter(fim))
            .collect(Collectors.toList());

        // Agrupamento do período
        Map<String, List<MatrizOD>> agrupado = pares.stream()
            .collect(Collectors.groupingBy(p -> p.getOriginStopId() + "|" + 
                                                (p.getDestinationStopId() != null ? p.getDestinationStopId() : "DESCONHECIDO") + "|" + 
                                                p.getRouteId() + "|" + 
                                                p.getPeriod()));

        List<Map<String, Object>> registrosAgregados = new ArrayList<>();
        for (Map.Entry<String, List<MatrizOD>> entry : agrupado.entrySet()) {
            List<MatrizOD> listaGrupo = entry.getValue();
            
            // Soma frequências (UC08.3)
            int totalVolume = listaGrupo.stream().mapToInt(MatrizOD::getVolume).sum();

            // Regra de privacidade: limiar mínimo de 5 ocorrências para publicar (UC08.3)
            if (totalVolume < 5) {
                continue;
            }

            // Remove outliers: fluxos fisicamente impossíveis num único dia (ex: volume acumulado excessivo > 10000)
            if (totalVolume > 10000) {
                continue;
            }

            // Recalcula confiança média (ALTO = 1.0, BAIXO = 0.5, INDETERMINADO = 0.0)
            double sumConfianca = 0;
            for (MatrizOD item : listaGrupo) {
                String c = item.getConfidenceIndex();
                if ("ALTO".equalsIgnoreCase(c)) sumConfianca += 1.0;
                else if ("BAIXO".equalsIgnoreCase(c)) sumConfianca += 0.5;
                else sumConfianca += 0.0;
            }
            double avgConfianca = sumConfianca / listaGrupo.size();
            String confiancaFinal = avgConfianca >= 0.75 ? "ALTO" : (avgConfianca >= 0.25 ? "BAIXO" : "INDETERMINADO");

            // Divide as chaves do agrupamento
            String[] parts = entry.getKey().split("\\|");
            String oId = parts[0];
            String dId = parts[1];
            String routeId = parts[2];
            String period = parts[3];

            // Resolve nomes das paragens
            String origemNome = stopRepository.findById(oId)
                .map(Paragem::getStopName)
                .orElse(oId);

            String destinoNome = dId.equals("DESCONHECIDO") ? "Desconhecido" : stopRepository.findById(dId)
                .map(Paragem::getStopName)
                .orElse(dId);

            Map<String, Object> reg = new LinkedHashMap<>();
            reg.put("origem_paragem",  origemNome);
            reg.put("destino_paragem", destinoNome);
            reg.put("volume",          totalVolume);
            reg.put("confiança",       confiancaFinal);
            reg.put("período",         period);
            registrosAgregados.add(reg);
        }

        // Ordena por volume decrescente
        registrosAgregados.sort((a, b) -> Integer.compare(
            ((Number) b.get("volume")).intValue(),
            ((Number) a.get("volume")).intValue()
        ));

        // UC08.3: Registar export em auditoria (quem exportou, quando, que dados)
        auditRepository.save(new RegistoAuditoriaIngestao(
            "EXPORT_MATRIZ_OD",
            "utilizador",
            user,
            String.format("Exportacao da Matriz O-D em formato %s. Periodo: %s a %s. Total de linhas agregadas: %d.",
                formato, inicio, fim, registrosAgregados.size()),
            OffsetDateTime.now()
        ));

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("dataInicio",    inicio);
        resposta.put("dataFim",       fim);
        resposta.put("totalRegistos", registrosAgregados.size());
        resposta.put("anonimizado",   true);
        resposta.put("formato",       formato);
        resposta.put("registos",      registrosAgregados);

        return ResponseEntity.ok(resposta);
    }
}