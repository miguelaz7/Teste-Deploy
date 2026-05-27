package pt.tub.ticketub.p2_ingestao_processamento_dados;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pt.tub.ticketub.p3_classificacao_tarifaria_rgpd.PoliticaAnonimizacao;
import pt.tub.ticketub.p3_classificacao_tarifaria_rgpd.RepositorioPoliticaAnonimizacao;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ControladorNormalizacaoAnonimizacaoTest {

    @Autowired
    private ControladorNormalizacaoAnonimizacao normalizacao;

    @Autowired
    private RepositorioPoliticaAnonimizacao policyRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        policyRepository.deleteAll();
    }

    @Test
    void testDefaultNormalizationAndAnonymization() {
        DtoPedidoIngestaoValidacao dto = new DtoPedidoIngestaoValidacao();
        dto.setCardId("123456789");
        dto.setTicketId("TICKET-RAW-123");
        dto.setTicketTypeCode("AVULSO");
        dto.setTransactionDateTime("2026-05-27T12:00:00Z");

        EventoValidacao result = normalizacao.normalize(dto, "dummy-hash");

        assertNotNull(result);
        assertNotEquals("123456789", result.getCardId());
        assertFalse(result.getCardId().isBlank());
        assertNotEquals("TICKET-RAW-123", result.getTicketId());
    }

    @Test
    void testDpoSuppressionPolicyForTicketId() {
        // Create active suppression policy for ticketId
        PoliticaAnonimizacao policy = new PoliticaAnonimizacao();
        policy.setField("ticketId");
        policy.setMethod("SUPRESSAO");
        policy.setRetentionDays(-1);
        policy.setStatus("ATIVA");
        policy.setApprovedBy("dpo@ticketub.pt");
        policy.setApprovedAt(OffsetDateTime.now());
        policy.setNotes("Suppress ticketId for GDPR compliance");
        policyRepository.save(policy);

        DtoPedidoIngestaoValidacao dto = new DtoPedidoIngestaoValidacao();
        dto.setCardId("123456789");
        dto.setTicketId("TICKET-RAW-123");
        dto.setTicketTypeCode("AVULSO");
        dto.setTransactionDateTime("2026-05-27T12:00:00Z");

        EventoValidacao result = normalizacao.normalize(dto, "dummy-hash");

        assertNotNull(result);
        assertNotEquals("123456789", result.getCardId());
        assertNull(result.getTicketId()); // Should be suppressed
    }

    @Test
    void testDpoSuppressionPolicyForCardId() {
        // Create active suppression policy for cardId
        PoliticaAnonimizacao policy = new PoliticaAnonimizacao();
        policy.setField("cardId");
        policy.setMethod("SUPRESSAO");
        policy.setRetentionDays(-1);
        policy.setStatus("ATIVA");
        policy.setApprovedBy("dpo@ticketub.pt");
        policy.setApprovedAt(OffsetDateTime.now());
        policy.setNotes("Suppress cardId completely");
        policyRepository.save(policy);

        DtoPedidoIngestaoValidacao dto = new DtoPedidoIngestaoValidacao();
        dto.setCardId("123456789");
        dto.setTicketId("TICKET-RAW-123");
        dto.setTicketTypeCode("AVULSO");
        dto.setTransactionDateTime("2026-05-27T12:00:00Z");

        EventoValidacao result = normalizacao.normalize(dto, "dummy-hash");

        assertNotNull(result);
        assertNull(result.getCardId()); // Should be suppressed
        assertNotEquals("TICKET-RAW-123", result.getTicketId());
    }
}
