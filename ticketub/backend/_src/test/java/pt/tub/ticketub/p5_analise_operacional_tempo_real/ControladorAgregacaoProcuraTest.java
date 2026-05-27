package pt.tub.ticketub.p5_analise_operacional_tempo_real;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;

import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Paragem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioParagem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioTipoBilhete;
import pt.tub.ticketub.p4_interfaces_utilizador.InterfaceProcuraTempoReal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ControladorAgregacaoProcuraTest {

    @Autowired
    private ControladorAgregacaoProcura demandController;

    @Autowired
    private InterfaceProcuraTempoReal demandInterface;

    @Autowired
    private RepositorioEventoValidacao eventRepository;

    @Autowired
    private RepositorioParagem stopRepository;

    @Autowired
    private RepositorioTipoBilhete ticketTypeRepository;

    @Autowired
    private RepositorioAgregadoProcura aggregateRepository;

    @Test
    public void testDemandAggregationAndMetrics() {
        // 1. Create and persist mock ticket types & stops
        TipoBilhete ticketType = ticketTypeRepository.save(new TipoBilhete("ESTUDANTE_PASS", "Passe Estudante"));
        
        Paragem stop1 = stopRepository.save(new Paragem("ST01", "Paragem Universidade", 41.5612, -8.3972, "ZONA_UNI"));
        Paragem stop2 = stopRepository.save(new Paragem("ST02", "Paragem Centro", 41.5501, -8.4212, "ZONA_CENTRO"));

        // 2. Create validation events
        OffsetDateTime now = OffsetDateTime.now();
        
        // 3 events for stop 1 (Uni)
        for (int i = 0; i < 3; i++) {
            EventoValidacao ev = new EventoValidacao();
            ev.setIngestionHash("hash-uni-" + i);
            ev.setIngestedAt(now);
            ev.setMediaType("CARD");
            ev.setTicketType(ticketType);
            ev.setTransactionType("VALIDATION");
            ev.setTransactionDateTime(now.withHour(8)); // hour 8
            ev.setOriginStop(stop1);
            ev.setRouteId("L10");
            ev.setResult("VALID");
            eventRepository.save(ev);
        }

        // 1 event for stop 2 (Centro)
        EventoValidacao evCentro = new EventoValidacao();
        evCentro.setIngestionHash("hash-centro");
        evCentro.setIngestedAt(now);
        evCentro.setMediaType("CARD");
        evCentro.setTicketType(ticketType);
        evCentro.setTransactionType("VALIDATION");
        evCentro.setTransactionDateTime(now.withHour(18)); // hour 18
        evCentro.setOriginStop(stop2);
        evCentro.setRouteId("L20");
        evCentro.setResult("VALID");
        eventRepository.save(evCentro);

        // 3. Trigger aggregation
        demandController.aggregate();

        // 4. Validate HORARIO aggregates
        List<AgregadoProcura> hourAggs = demandController.getByPerspective("HORARIO");
        assertFalse(hourAggs.isEmpty());
        
        AgregadoProcura peakHourAgg = hourAggs.stream()
            .filter(a -> "8".equals(a.getChave()))
            .findFirst()
            .orElse(null);
        assertNotNull(peakHourAgg);
        assertEquals(3, peakHourAgg.getTotalValidacoes());
        assertNotNull(peakHourAgg.getBaselineMedia());
        assertNotNull(peakHourAgg.getVariacaoBaseline());
        assertNotNull(peakHourAgg.getDesvioDetectado());

        // 5. Validate LINHA aggregates (velocity & response time)
        List<AgregadoProcura> routeAggs = demandController.getByPerspective("LINHA");
        assertFalse(routeAggs.isEmpty());
        
        AgregadoProcura routeAgg = routeAggs.stream()
            .filter(a -> "L10".equals(a.getChave()))
            .findFirst()
            .orElse(null);
        assertNotNull(routeAgg);
        assertEquals(3, routeAgg.getTotalValidacoes());
        // Since we registered 3 events in the last 5 minutes, velocity should be 3/5 = 0.6
        assertEquals(0.6, routeAgg.getVelocidadeValidacao(), 0.01);
        assertTrue(routeAgg.getTempoRespostaMedio() >= 150.0);

        // 6. Validate ZONA_PARAGEM coordinates
        List<AgregadoProcura> stopAggs = demandController.getByPerspective("ZONA_PARAGEM");
        assertFalse(stopAggs.isEmpty());
        
        AgregadoProcura stopAgg = stopAggs.stream()
            .filter(a -> "ST01".equals(a.getChave()))
            .findFirst()
            .orElse(null);
        assertNotNull(stopAgg);
        assertEquals(41.5612, stopAgg.getStopLat(), 0.0001);
        assertEquals(-8.3972, stopAgg.getStopLon(), 0.0001);
        assertEquals("ZONA_UNI", stopAgg.getZoneId());

        // 7. Validate ZONA perspective aggregation (UC05.3)
        List<AgregadoProcura> zoneAggs = demandController.getByPerspective("ZONA");
        assertFalse(zoneAggs.isEmpty());
        
        AgregadoProcura zoneAgg = zoneAggs.stream()
            .filter(a -> "ZONA_UNI".equals(a.getChave()))
            .findFirst()
            .orElse(null);
        assertNotNull(zoneAgg);
        assertEquals(3, zoneAgg.getTotalValidacoes());
        // Geolocation of zone should match the coordinates of the stop
        assertEquals(41.5612, zoneAgg.getStopLat(), 0.0001);
        assertEquals(-8.3972, zoneAgg.getStopLon(), 0.0001);

        // 8. Test Controller Endpoint for Zone perspective
        ResponseEntity<List<?>> zoneResponse = demandInterface.getDemandByZone();
        assertEquals(200, zoneResponse.getStatusCode().value());
        assertNotNull(zoneResponse.getBody());
        assertFalse(zoneResponse.getBody().isEmpty());
    }
}
