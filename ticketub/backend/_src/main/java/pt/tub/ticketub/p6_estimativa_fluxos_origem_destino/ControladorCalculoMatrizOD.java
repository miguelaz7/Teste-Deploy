package pt.tub.ticketub.p6_estimativa_fluxos_origem_destino;

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Trip;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TripRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// =============================================================================
// O0.8.1.c – Controlador de Cálculo da Matriz O-D (UC08.1)
// Agrega validações georreferenciadas, reconstitui sequências de viagem
// por card_id pseudonimizado e estima destinos por prioridade:
//   1.ª prioridade — validação seguinte do mesmo card_id
//   2.ª prioridade — terminus da linha como fallback
// Executa diariamente às 02h00, disponibilizando a matriz antes das 07h00.
// Consome dados de: O0.7.1.d (validações georreferenciadas via ValidationEvent).
// Produz dados em: O0.8.1.d (MatrizODRepository).
// =============================================================================

@Service
@RestController
@RequestMapping("/api/od")
public class ControladorCalculoMatrizOD {

    private final ValidationEventRepository validationEventRepository;
    private final MatrizODRepository matrizODRepository;
    private final TripRepository tripRepository;

    ControladorCalculoMatrizOD(ValidationEventRepository validationEventRepository,
                               MatrizODRepository matrizODRepository,
                               TripRepository tripRepository) {
        this.validationEventRepository = validationEventRepository;
        this.matrizODRepository        = matrizODRepository;
        this.tripRepository            = tripRepository;
    }

    // Executa diariamente às 02h00 — matriz disponível antes das 07h00 (UC08.1)
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    void calcularMatrizDiaria() {
        LocalDate ontem = LocalDate.now().minusDays(1);

        // Evita recalcular se já existe matriz para esta data
        if (matrizODRepository.existsByDataCalculo(ontem)) {
            return;
        }

        calcularParaData(ontem);
    }

    // Permite recálculo manual para uma data específica
    @Transactional
    void calcularParaData(LocalDate data) {
        OffsetDateTime inicioDia = data.atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
        OffsetDateTime fimDia    = data.plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);

        // Carrega apenas eventos com paragem de origem georreferenciada (de O0.7.1.d)
        List<ValidationEvent> eventos = validationEventRepository
            .findAll().stream()
            .filter(e -> e.getTransactionDateTime() != null
                && !e.getTransactionDateTime().isBefore(inicioDia)
                && e.getTransactionDateTime().isBefore(fimDia)
                && e.getOriginStop() != null
                && e.getCardId() != null)
            .sorted(Comparator.comparing(ValidationEvent::getTransactionDateTime))
            .collect(Collectors.toList());

        // Agrupa por card_id pseudonimizado — cada grupo é a sequência de viagem do dia
        Map<String, List<ValidationEvent>> porCartao = eventos.stream()
            .collect(Collectors.groupingBy(ValidationEvent::getCardId));

        // Acumula pares O-D: chave = "origemId|destinoId|routeId|periodo"
        Map<String, int[]> contagens = new java.util.LinkedHashMap<>();
        Map<String, String[]> metadados = new java.util.LinkedHashMap<>();

        for (List<ValidationEvent> sequencia : porCartao.values()) {
            processarSequencia(sequencia, data, contagens, metadados);
        }

        // Persiste todos os pares calculados
        List<MatrizOD> pares = new ArrayList<>();
        for (Map.Entry<String, int[]> entrada : contagens.entrySet()) {
            String[] meta   = metadados.get(entrada.getKey());
            int volume      = entrada.getValue()[0];
            String origem   = meta[0];
            String destino  = meta[1];   // null se desconhecido
            String routeId  = meta[2];
            String periodo  = meta[3];
            String confianca = meta[4];

            pares.add(new MatrizOD(
                origem,
                destino,
                destino == null,
                routeId,
                periodo,
                data,
                volume,
                confianca,
                OffsetDateTime.now()
            ));
        }

        matrizODRepository.saveAll(pares);
    }

    // -------------------------------------------------------------------------
    // Processamento de uma sequência de viagem de um card_id
    // -------------------------------------------------------------------------

    private void processarSequencia(List<ValidationEvent> sequencia, LocalDate data,
                                    Map<String, int[]> contagens,
                                    Map<String, String[]> metadados) {
        for (int i = 0; i < sequencia.size(); i++) {
            ValidationEvent origem = sequencia.get(i);
            String origemStopId   = origem.getOriginStop().getStopId();
            String routeId        = origem.getRouteId();
            String periodo        = classificarPeriodo(origem.getTransactionDateTime());

            String destinoStopId;
            String confianca;

            // 1.ª prioridade: validação seguinte do mesmo card_id
            if (i + 1 < sequencia.size()) {
                ValidationEvent proxima = sequencia.get(i + 1);
                if (proxima.getOriginStop() != null) {
                    destinoStopId = proxima.getOriginStop().getStopId();
                    confianca     = "ALTO";
                } else {
                    // 2.ª prioridade: terminus da linha como fallback
                    destinoStopId = resolverTerminus(routeId, origem.getTripId());
                    confianca     = "BAIXO";
                }
            } else {
                // Última validação do dia — sem validação seguinte
                destinoStopId = resolverTerminus(routeId, origem.getTripId());
                confianca     = destinoStopId != null ? "BAIXO" : "INDETERMINADO";
            }

            // Destino desconhecido se não foi possível estimar
            String chave = origemStopId + "|" + destinoStopId + "|" + routeId + "|" + periodo;

            contagens.computeIfAbsent(chave, k -> new int[]{0})[0]++;
            metadados.putIfAbsent(chave,
                new String[]{origemStopId, destinoStopId, routeId, periodo, confianca});
        }
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    // Terminus da linha: última paragem do trip (fallback quando sem validação seguinte)
    private String resolverTerminus(String routeId, String tripId) {
        if (tripId == null) return null;
        Optional<Trip> trip = tripRepository.findById(tripId);
        return trip.map(Trip::getLastStopId).orElse(null);
    }

    // Classifica o período do dia conforme UC08.1
    private String classificarPeriodo(OffsetDateTime dt) {
        if (dt == null) return "VAZIO";
        LocalTime hora = dt.toLocalTime();
        int h = hora.getHour();

        // Fim de semana
        java.time.DayOfWeek diaSemana = dt.getDayOfWeek();
        if (diaSemana == java.time.DayOfWeek.SATURDAY
                || diaSemana == java.time.DayOfWeek.SUNDAY) {
            return "FIM_SEMANA";
        }
        // Ponta manhã: 07h–09h
        if (h >= 7 && h < 9)  return "PONTA_MANHA";
        // Ponta tarde: 17h–19h
        if (h >= 17 && h < 19) return "PONTA_TARDE";
        return "VAZIO";
    }

    @PostMapping("/forcar-calculo")
    public ResponseEntity<Map<String, Object>> forcarCalculo() {
        try {
            // Forçar cálculo para hoje (não ontem como no agendado)
            calcularParaData(java.time.LocalDate.now());
            return ResponseEntity.ok(Map.of(
                "status", "sucesso",
                "mensagem", "Calculo da Matriz O-D concluido.",
                "data", java.time.LocalDate.now().toString()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "erro",
                "mensagem", e.getMessage() != null ? e.getMessage() : "Erro desconhecido"
            ));
        }
    }
}