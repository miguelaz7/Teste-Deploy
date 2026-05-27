package pt.tub.ticketub.p1_autenticacao_controlo_acesso;

// UC01.2 – Validar token e extrair atributos relevantes
// UC01.3 – Aplicar mapeamento de papéis para perfis internos
// UC01.4 – Registar auditoria de autenticação

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ControladorAutenticacao {

    private final ServicoAuditoria servicoAuditoria;

    // UC01.3 – Tabela de mapeamento: papel do IdP → perfil interno
    private static final Map<String, String> MAPEAMENTO_PERFIS = Map.of(
        "tub-gestor",   "GESTOR",
        "tub-operador", "OPERADOR",
        "tub-analista", "ANALISTA",
        "tub-dpo",      "DPO",
        "tub-admin",    "ADMIN",
        // Suporte a variantes minúsculas diretas
        "gestor",       "GESTOR",
        "operador",     "OPERADOR",
        "analista",     "ANALISTA"
    );

    public ControladorAutenticacao(ServicoAuditoria servicoAuditoria) {
        this.servicoAuditoria = servicoAuditoria;
    }

    /**
     * UC01.2 – Endpoint que valida o token JWT (já validado pelo Spring Security),
     * extrai atributos (sub, email, nome, grupos/papéis) e executa UC01.3 e UC01.4.
     */
    @GetMapping("/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            // FA3 – Token ausente ou inválido
            servicoAuditoria.registarLoginFalha("desconhecido", "TOKEN_INVALIDO", "JWT nulo no endpoint /api/profile");
            return Map.of("erro", "Token inválido ou sessão expirada.");
        }

        // UC01.2 – Extrair atributos do payload do JWT
        String sub   = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String nome  = jwt.getClaimAsString("name");

        // UC01.2 – Extrair grupos/papéis (claim customizado configurado no Auth0)
        List<String> rawRoles = jwt.getClaimAsStringList("https://ticketub.pt/roles");
        if (rawRoles == null) rawRoles = jwt.getClaimAsStringList("roles");
        if (rawRoles == null) rawRoles = jwt.getClaimAsStringList("permissions");
        if (rawRoles == null) rawRoles = new ArrayList<>();

        // UC01.3 – Mapear papéis do IdP para perfis internos (maior privilégio)
        List<String> perfisInternos = mapearPerfis(rawRoles);

        // UC01.3 / FA4 – Sem perfil mapeado: bloquear e notificar
        if (perfisInternos.isEmpty()) {
            servicoAuditoria.registarSemPerfil(sub, email != null ? email : sub);
            return Map.of(
                "erro", "Acesso negado: utilizador sem perfil autorizado atribuído. Contacte o administrador."
            );
        }

        // UC01.4 – Registar LOGIN_SUCCESS com hora de expiração
        Instant exp = jwt.getExpiresAt();
        OffsetDateTime expiracaoSessao = exp != null ? exp.atOffset(ZoneOffset.UTC) : null;
        servicoAuditoria.registarLoginSucesso(sub, email, nome, String.join(", ", perfisInternos), expiracaoSessao);

        // Resposta para o frontend (UC01.3 – perfis aplicados)
        Map<String, Object> resposta = new HashMap<>();
        resposta.put("sub",    sub);
        resposta.put("email",  email != null ? email : "");
        resposta.put("name",   nome  != null ? nome  : "");
        resposta.put("roles",  perfisInternos);
        return resposta;
    }

    /**
     * UC01.3 – Mapeia papéis do IdP para perfis internos com agregação de maior privilégio.
     */
    private List<String> mapearPerfis(List<String> rawRoles) {
        List<String> perfis = new ArrayList<>();
        for (String role : rawRoles) {
            String normalizado = role.toLowerCase()
                .replace("role_", "")
                .trim();
            String perfilInterno = MAPEAMENTO_PERFIS.get(normalizado);
            if (perfilInterno != null && !perfis.contains(perfilInterno)) {
                perfis.add(perfilInterno);
            }
        }
        // UC01.3 – Fallback para utilizadores Auth0 sem roles explícitas configuradas
        // (ex: contas de teste criadas no painel Auth0 sem Actions configuradas)
        if (perfis.isEmpty() && !rawRoles.isEmpty()) {
            perfis.add("OPERADOR"); // perfil mínimo por defeito
        }
        return perfis;
    }

    @GetMapping("/public/hello")
    public String hello() {
        return "Endpoint público do TickeTUB. Sem autenticação necessária.";
    }
}
