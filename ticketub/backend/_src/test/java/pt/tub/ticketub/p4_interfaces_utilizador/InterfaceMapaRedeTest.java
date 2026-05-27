package pt.tub.ticketub.p4_interfaces_utilizador;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Paragem;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioParagem;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import org.springframework.http.ResponseEntity;

import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class InterfaceMapaRedeTest {

    @Autowired
    private InterfaceMapaRede interfaceMapaRede;

    @Autowired
    private RepositorioParagem stopRepository;

    @Autowired
    private RepositorioEventoValidacao validationEventRepository;

    @Autowired
    private RepositorioQuarentenaValidacao validationQuarantineRepository;

    @Autowired
    private pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioTipoBilhete ticketTypeRepository;

    private Paragem testStop;

    @BeforeEach
    void setUp() {
        validationEventRepository.deleteAll();
        if (stopRepository.existsById("STOP-999")) {
            stopRepository.deleteById("STOP-999");
        }

        // Create test stop with isolated coordinates (45.0, -10.0)
        testStop = new Paragem();
        testStop.setStopId("STOP-999");
        testStop.setStopName("Paragem Teste UC06");
        testStop.setStopLat(45.0);
        testStop.setStopLon(-10.0);
        stopRepository.save(testStop);
    }

    @AfterEach
    void tearDown() {
        validationEventRepository.deleteAll();
        if (stopRepository.existsById("STOP-999")) {
            stopRepository.deleteById("STOP-999");
        }
    }

    @Test
    void testGetStopByCoordinatesWithin50m() {
        // Coordinate close to stop: within ~5 meters (45.00003, -10.00003)
        ResponseEntity<Map<String, Object>> response = interfaceMapaRede.getStopByCoordinates(45.00003, -10.00003);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("STOP-999", response.getBody().get("stopId"));
        assertEquals("Paragem Teste UC06", response.getBody().get("stopName"));
    }

    @Test
    void testGetStopByCoordinatesOutside50m() {
        // Coordinate far from stop
        ResponseEntity<Map<String, Object>> response = interfaceMapaRede.getStopByCoordinates(45.01000, -10.01000);
        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void testAfluenciaStatusCalculation() {
        OffsetDateTime agora = OffsetDateTime.now();
        
        pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete tb = ticketTypeRepository.findAll().stream().findFirst().orElseGet(() -> {
            pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete t = new pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete();
            t.setCode("AVULSO");
            t.setDescription("Avulso Test");
            return ticketTypeRepository.save(t);
        });

        for (int i = 0; i < 5; i++) {
            EventoValidacao ev = new EventoValidacao();
            ev.setCardId("CARD-" + i);
            ev.setTicketId("TKT-" + i);
            ev.setResult("VALID");
            ev.setOriginStop(testStop);
            ev.setTransactionDateTime(agora.minusMinutes(1));
            ev.setIngestionHash("hash-" + i);
            ev.setIngestedAt(agora);
            ev.setMediaType("MEDIA_CARD");
            ev.setTransactionType("VALIDACAO");
            ev.setTicketType(tb);
            validationEventRepository.save(ev);
        }

        ResponseEntity<Map<String, Object>> response = interfaceMapaRede.getLiveData("STOP-999");
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        
        assertEquals(5L, response.getBody().get("totalValidations"));
        assertNotNull(response.getBody().get("afluenciaStatus"));
    }
}
