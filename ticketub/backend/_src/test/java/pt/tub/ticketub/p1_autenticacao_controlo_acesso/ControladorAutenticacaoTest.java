package pt.tub.ticketub.p1_autenticacao_controlo_acesso;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
public class ControladorAutenticacaoTest {

    @Autowired
    private ControladorAutenticacao controlador;

    @Autowired
    private RepositorioAuditoriaIngestao auditRepository;

    @Test
    public void testPublicHelloEndpointDirect() {
        String res = controlador.hello();
        assertNotNull(res);
        assertTrue(res.contains("público"));
    }

    @Test
    public void testProfileEndpointDirect() {
        // Setup mock JWT
        Jwt jwt = Mockito.mock(Jwt.class);
        Mockito.when(jwt.getClaimAsString("email")).thenReturn("admin@ticketub.pt");
        Mockito.when(jwt.getClaimAsString("name")).thenReturn("Admin User");
        Mockito.when(jwt.getSubject()).thenReturn("admin-sub-123");

        // Mock audit repository save behavior
        Mockito.when(auditRepository.save(any(RegistoAuditoriaIngestao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> profile = controlador.profile(jwt, null);

        assertNotNull(profile);
        assertEquals("Admin User", profile.get("name"));
        assertEquals("admin@ticketub.pt", profile.get("email"));
        assertEquals("admin-sub-123", profile.get("sub"));

        // Verify audit log registry was triggered
        Mockito.verify(auditRepository, Mockito.atLeastOnce()).save(any(RegistoAuditoriaIngestao.class));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public RepositorioAuditoriaIngestao mockAuditRepository() {
            return Mockito.mock(RepositorioAuditoriaIngestao.class);
        }
    }
}
