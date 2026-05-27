package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.Jwt;
import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ServicoSuspensaoLote;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ControladorNormalizacaoAnonimizacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.DtoPedidoIngestaoValidacao;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRegistoDataLakeNgsiLd;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RegistoDataLakeNgsiLd;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.TipoBilhete;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioTipoBilhete;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioAlerta;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.Alerta;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.QuarentenaValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import pt.tub.ticketub.p4_interfaces_utilizador.InterfaceGestaoAlertas;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ClassificacaoRgpdTest {

    @Autowired
    private ControladorClassificacaoTarifaria classificationController;

    @Autowired
    private RepositorioEventoValidacao eventRepository;

    @Autowired
    private RepositorioRegistoDataLakeNgsiLd dataLakeRepository;

    @Autowired
    private RepositorioAlerta alertRepository;

    @Autowired
    private RepositorioAuditoriaCategorizacao auditRepository;

    @Autowired
    private ControladorNormalizacaoAnonimizacao normalizationController;

    @Autowired
    private RepositorioMapeamentoTipologiaPerfil mappingRepository;

    @Autowired
    private RepositorioTipoBilhete ticketTypeRepository;

    @Autowired
    private RepositorioQuarentenaValidacao quarantineRepository;

    @Autowired
    private InterfaceGestaoAlertas alertsInterface;

    @Test
    public void testPiiDetectionAndBatchSuspension() {
        // Create an event that contains PII (email address) in cardId
        String ingestionHash = UUID.randomUUID().toString();
        String batchId = "batch-" + UUID.randomUUID();

        // 1. Create DataLake registry to map ingestionHash to batchId
        RegistoDataLakeNgsiLd dlEntry = new RegistoDataLakeNgsiLd(
            batchId,
            "urn:ngsi-ld:FareTransaction:" + ingestionHash,
            "FareTransaction",
            "{}",
            java.time.LocalDate.now(),
            OffsetDateTime.now(),
            41.5,
            -8.4,
            OffsetDateTime.now()
        );
        dataLakeRepository.save(dlEntry);

        TipoBilhete ticketType = ticketTypeRepository.findByCode("NORMAL")
            .orElseGet(() -> ticketTypeRepository.save(new TipoBilhete("NORMAL", "NORMAL")));

        // 2. Create EventoValidacao with PII email
        EventoValidacao event = new EventoValidacao();
        event.setIngestionHash(ingestionHash);
        event.setCardId("user_email@ticketub.pt"); // Email PII
        event.setTicketId("ticket-123");
        event.setMediaType("NFC_SMARTCARD");
        event.setTicketType(ticketType);
        event.setTransactionType("VALIDATION");
        event.setResult("OK");
        event.setIngestedAt(OffsetDateTime.now());
        event.setTransactionDateTime(OffsetDateTime.now());
        event.setPiiDetected(false);

        eventRepository.save(event);

        // 3. Classify
        classificationController.classify();

        // 4. Assert
        EventoValidacao processed = eventRepository.findAll().stream()
            .filter(e -> ingestionHash.equals(e.getIngestionHash()))
            .findFirst()
            .orElseThrow();

        assertTrue(processed.getPiiDetected());
        assertEquals("SUSPENSO", processed.getPerfilClassificado());

        // Check if DPO alert was created
        List<Alerta> alerts = alertRepository.findAll();
        boolean alertFound = alerts.stream()
            .anyMatch(a -> "DETECAO_PII".equals(a.getType()) && a.getEventData().contains(ingestionHash));
        assertTrue(alertFound, "DPO alert for PII detection not found");

        // Check audit log
        List<AuditoriaCategorizacao> audits = auditRepository.findAll();
        boolean auditFound = audits.stream()
            .anyMatch(a -> "PII_DETECTION".equals(a.getAcao()));
        assertTrue(auditFound, "PII audit log not found");
    }

    @Test
    public void testContextualGeographicMasking() {
        // Setup a mapping for a sensitive category, e.g. "Senior" or "Estudante"
        MapeamentoTipologiaPerfil mapping = new MapeamentoTipologiaPerfil();
        mapping.setTipoTitulo("SENIOR_TICKET");
        mapping.setPerfil("senior");
        mappingRepository.save(mapping);

        String ingestionHash = UUID.randomUUID().toString();
        
        TipoBilhete ticketType = ticketTypeRepository.findByCode("SENIOR_TICKET")
            .orElseGet(() -> ticketTypeRepository.save(new TipoBilhete("SENIOR_TICKET", "Passe Senior")));

        EventoValidacao event = new EventoValidacao();
        event.setIngestionHash(ingestionHash);
        event.setCardId("pseudonymized-card-id");
        event.setTicketId("pseudonymized-ticket-id");
        event.setMediaType("NFC_SMARTCARD");
        event.setTicketType(ticketType);
        event.setTransactionType("VALIDATION");
        event.setResult("OK");
        event.setIngestedAt(OffsetDateTime.now());
        event.setTransactionDateTime(OffsetDateTime.now());
        
        // We set originStop to something, it should be masked/cleared during classification because "senior" is a sensitive category
        eventRepository.save(event);

        classificationController.classify();

        EventoValidacao processed = eventRepository.findAll().stream()
            .filter(e -> ingestionHash.equals(e.getIngestionHash()))
            .findFirst()
            .orElseThrow();

        assertEquals("senior", processed.getPerfilClassificado());
        assertNull(processed.getOriginStop());
        assertTrue(processed.getMaskedFields().contains("originStop(SENSITIVE_MASK)"));
    }

    @Test
    public void testMinimizationDefaultTicketIdPseudonymization() {
        DtoPedidoIngestaoValidacao dto = new DtoPedidoIngestaoValidacao();
        dto.setCardId("11223344");
        dto.setTicketId("998877");
        dto.setMediaType("NFC_SMARTCARD");
        dto.setTicketTypeCode("PASSE_ESTUDANTE");
        dto.setTransactionType("VALIDATION");
        dto.setTransactionDateTime(OffsetDateTime.now().toString());
        dto.setResult("OK");

        String testHash = UUID.randomUUID().toString();
        EventoValidacao event = normalizationController.normalize(dto, testHash);

        // ticketId should be pseudonymized (default minimization rule), not equal to original raw "998877"
        assertNotNull(event.getTicketId());
        assertNotEquals("998877", event.getTicketId());
        assertTrue(event.getMaskedFields().contains("ticketId(DEFAULT_HMAC_SHA256)"));
        assertNotNull(event.getMaskedFields());
    }

    @Test
    public void testAlertFilteringAndSorting() {
        // Clear prior quarantine records to have clean slate for test asserts
        quarantineRepository.deleteAll();

        // 1. Technical error
        QuarentenaValidacao tech = new QuarentenaValidacao(
            "{\"routeId\":\"L10\",\"originStopId\":\"S1\"}",
            "SYSTEM_ERROR",
            "system",
            "null",
            "technical_error_rule",
            "S1",
            OffsetDateTime.now().minusMinutes(5)
        );
        quarantineRepository.save(tech);

        // 2. Operational error (INVALID_TICKET)
        QuarentenaValidacao op1 = new QuarentenaValidacao(
            "{\"routeId\":\"L20\",\"originStopId\":\"S2\"}",
            "INVALID_TICKET",
            "ticket",
            "999",
            "operational_error_rule",
            "S2",
            OffsetDateTime.now().minusMinutes(2)
        );
        quarantineRepository.save(op1);

        // 3. Operational error (UNAUTHORIZED_ZONE)
        QuarentenaValidacao op2 = new QuarentenaValidacao(
            "{\"routeId\":\"L30\",\"originStopId\":\"S3\"}",
            "UNAUTHORIZED_ZONE",
            "zone",
            "3",
            "operational_error_rule",
            "S3",
            OffsetDateTime.now()
        );
        quarantineRepository.save(op2);

        // Mock JWTs
        Jwt gestorJwt = Jwt.withTokenValue("mock-gestor-token")
            .header("alg", "none")
            .claim("https://ticketub.pt/roles", List.of("gestor"))
            .subject("test-gestor")
            .build();

        Jwt adminJwt = Jwt.withTokenValue("mock-admin-token")
            .header("alg", "none")
            .claim("https://ticketub.pt/roles", List.of("admin"))
            .subject("test-admin")
            .build();

        // 4. Test RBAC filter for Gestor (Technical error should be filtered out)
        List<Map<String, Object>> gestorList = alertsInterface.getQuarantine(24, gestorJwt).getBody();
        assertNotNull(gestorList);
        assertEquals(2, gestorList.size());
        boolean hasTech = gestorList.stream().anyMatch(m -> "SYSTEM_ERROR".equals(m.get("motivo")));
        assertFalse(hasTech, "Gestor should not see technical alerts");

        // 5. Test RBAC filter for Admin (Should see all)
        List<Map<String, Object>> adminList = alertsInterface.getQuarantine(24, adminJwt).getBody();
        assertNotNull(adminList);
        assertEquals(3, adminList.size());

        // 6. Test severity sorting (CRITICO -> SYSTEM_ERROR, should be first)
        Map<String, Object> first = adminList.get(0);
        assertEquals("SYSTEM_ERROR", first.get("motivo"));
        assertEquals("CRITICO", first.get("severidade"));

        // 7. Test context injection
        Map<String, Object> second = adminList.get(1); // The most recent operational error
        assertEquals("L30", second.get("linhaAfetada"));
        assertEquals("S3", second.get("zona"));
        assertNotNull(second.get("ultimaAcaoTomada"));
    }
}
