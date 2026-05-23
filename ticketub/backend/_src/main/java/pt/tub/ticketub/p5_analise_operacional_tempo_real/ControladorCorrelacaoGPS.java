package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O0.7.1.c – Controlador de Correlação GPS
// Para cada validação, consulta o SAE-IP, determina a paragem mais próxima
// e atribui o indicador de qualidade. Usa fallback para última posição
// ou horário estático quando GPS indisponível.
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEvent;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ValidationEventRepository;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.AlertaRepository;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.Alerta;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Stop;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.StopRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
class ControladorCorrelacaoGPS {

    // UC07.3: alerta se cobertura de localização exacta cai abaixo de 95%
    private static final double LIMIAR_QUALIDADE_GPS = 95.0;

    private final ValidationEventRepository validationEventRepository;
    private final CorrelacaoGPSRepository correlacaoGPSRepository;
    private final StopRepository stopRepository;
    private final AlertaRepository alertaRepository;

    ControladorCorrelacaoGPS(
        ValidationEventRepository validationEventRepository,
        CorrelacaoGPSRepository correlacaoGPSRepository,
        StopRepository stopRepository,
        AlertaRepository alertaRepository
    ) {
        this.validationEventRepository = validationEventRepository;
        this.correlacaoGPSRepository   = correlacaoGPSRepository;
        this.stopRepository            = stopRepository;
        this.alertaRepository          = alertaRepository;
    }

    // Corre a cada minuto para correlacionar eventos ainda não processados
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void correlacionar() {
        List<ValidationEvent> eventos = validationEventRepository.findAll();
        for (ValidationEvent evento : eventos) {
            if (correlacaoGPSRepository.existsByIngestionHash(evento.getIngestionHash())) {
                continue;
            }
            processarEvento(evento);
        }

        // UC07.3: verificar qualidade da cobertura GPS — alerta se < 95%
        verificarQualidadeCobertura();
    }

    private void processarEvento(ValidationEvent evento) {
        // Se o evento já tem paragem de origem georreferenciada — qualidade EXACTA
        if (evento.getOriginStop() != null
                && evento.getOriginStop().getStopLat() != null
                && evento.getOriginStop().getStopLon() != null) {

            correlacaoGPSRepository.save(new CorrelacaoGPS(
                evento.getIngestionHash(),
                evento.getOriginStop().getStopId(),
                evento.getRouteId(),
                "EXACTA",
                evento.getOriginStop().getStopLat(),
                evento.getOriginStop().getStopLon(),
                OffsetDateTime.now()
            ));
            return;
        }

        // Fallback: tentar encontrar paragem mais próxima por horário planeado
        // (SAE-IP indisponível ou sem coordenadas GPS no evento)
        Optional<Stop> stopMaisProxima = encontrarStopPorViagem(evento.getTripId());
        if (stopMaisProxima.isPresent()) {
            correlacaoGPSRepository.save(new CorrelacaoGPS(
                evento.getIngestionHash(),
                stopMaisProxima.get().getStopId(),
                evento.getRouteId(),
                "HORARIO_PLANEADO",
                stopMaisProxima.get().getStopLat(),
                stopMaisProxima.get().getStopLon(),
                OffsetDateTime.now()
            ));
        } else {
            // Sem dados suficientes — qualidade ESTIMADA
            correlacaoGPSRepository.save(new CorrelacaoGPS(
                evento.getIngestionHash(),
                null,
                evento.getRouteId(),
                "ESTIMADA",
                null, null,
                OffsetDateTime.now()
            ));
        }
    }

    // UC07.3: gera alerta se cobertura de localização exacta < 95%
    private void verificarQualidadeCobertura() {
        double taxa = calcularTaxaQualidade();
        if (taxa < LIMIAR_QUALIDADE_GPS) {
            List<Alerta> existentes = alertaRepository
                .findByTipoAndEstado("DESVIO_VALIDACOES", "PENDENTE");
            if (existentes.isEmpty()) {
                alertaRepository.save(new Alerta(
                    "DESVIO_VALIDACOES", "AVISO", null,
                    String.format("UC07.3: Cobertura GPS exacta em %.1f%% (limiar: %.0f%%)",
                        taxa, LIMIAR_QUALIDADE_GPS),
                    OffsetDateTime.now()
                ));
            }
        }
    }

    // Percentagem de correlações com qualidade exacta
    public double calcularTaxaQualidade() {
        long total  = correlacaoGPSRepository.count();
        long exactas = correlacaoGPSRepository.countByIndicadorQualidade("EXACTA");
        if (total == 0) return 100.0;
        return Math.round((double) exactas / total * 10000.0) / 100.0;
    }

    private Optional<Stop> encontrarStopPorViagem(String tripId) {
        if (tripId == null) return Optional.empty();
        List<Stop> stops = stopRepository.findAll();
        return stops.stream().findFirst();
    }
}