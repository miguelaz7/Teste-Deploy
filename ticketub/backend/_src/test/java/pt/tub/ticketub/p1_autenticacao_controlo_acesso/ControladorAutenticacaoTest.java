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

import java.util.List;
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

    /**
     * UC01.2 + UC01.3 – Token válido com perfil mapeado: deve retornar dados do utilizador
     * com o perfil interno correto (tub-gestor → GESTOR).
     */
    @Test
    public void testProfileEndpointComPerfilMapeado() {
        // Setup mock JWT (UC01.2 – simula atributos extraídos do token Auth0)
        Jwt jwt = Mockito.mock(Jwt.class);
        Mockito.when(jwt.getClaimAsString("email")).thenReturn("gestor@ticketub.pt");
        Mockito.when(jwt.getClaimAsString("name")).thenReturn("Gestor TUB");
        Mockito.when(jwt.getSubject()).thenReturn("auth0|gestor-sub-123");
        // UC01.2 – grupo/papel no claim customizado
        Mockito.when(jwt.getClaimAsStringList("https://ticketub.pt/roles")).thenReturn(List.of("tub-gestor"));
        Mockito.when(jwt.getClaimAsStringList("roles")).thenReturn(null);
        Mockito.when(jwt.getClaimAsStringList("permissions")).thenReturn(null);
        Mockito.when(jwt.getExpiresAt()).thenReturn(null);

        // Mock audit repository
        Mockito.when(auditRepository.save(any(RegistoAuditoriaIngestao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> profile = controlador.profile(jwt);

        assertNotNull(profile);
        assertEquals("Gestor TUB", profile.get("name"));
        assertEquals("gestor@ticketub.pt", profile.get("email"));
        assertEquals("auth0|gestor-sub-123", profile.get("sub"));
        // UC01.3 – papel mapeado corretamente
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) profile.get("roles");
        assertNotNull(roles);
        assertTrue(roles.contains("GESTOR"));

        // UC01.4 – evento de auditoria registado
        Mockito.verify(auditRepository, Mockito.atLeastOnce()).save(any(RegistoAuditoriaIngestao.class));
    }

    /**
     * UC01 / FA4 – Utilizador autenticado mas sem perfil associado: deve retornar erro
     * e registar evento de auditoria LOGIN_FAILED_SEM_PERFIL.
     */
    @Test
    public void testProfileEndpointSemPerfil() {
        Jwt jwt = Mockito.mock(Jwt.class);
        Mockito.when(jwt.getClaimAsString("email")).thenReturn("semrole@ticketub.pt");
        Mockito.when(jwt.getClaimAsString("name")).thenReturn("Sem Perfil");
        Mockito.when(jwt.getSubject()).thenReturn("auth0|semrole-sub");
        // Sem roles definidos
        Mockito.when(jwt.getClaimAsStringList("https://ticketub.pt/roles")).thenReturn(List.of());
        Mockito.when(jwt.getClaimAsStringList("roles")).thenReturn(null);
        Mockito.when(jwt.getClaimAsStringList("permissions")).thenReturn(null);
        Mockito.when(jwt.getExpiresAt()).thenReturn(null);

        Mockito.when(auditRepository.save(any(RegistoAuditoriaIngestao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> profile = controlador.profile(jwt);

        // FA4 – deve devolver mensagem de erro, não dados do utilizador
        assertNotNull(profile);
        assertTrue(profile.containsKey("erro"));

        // UC01.4 – evento de falha deve ter sido registado
        Mockito.verify(auditRepository, Mockito.atLeastOnce()).save(any(RegistoAuditoriaIngestao.class));
    }

    /**
     * UC01 / FA3 – JWT nulo (token inválido ou expirado): deve retornar erro.
     */
    @Test
    public void testProfileEndpointTokenNulo() {
        Mockito.when(auditRepository.save(any(RegistoAuditoriaIngestao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> profile = controlador.profile(null);

        assertNotNull(profile);
        assertTrue(profile.containsKey("erro"));
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
