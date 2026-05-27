package pt.tub.ticketub.p1_autenticacao_controlo_acesso;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ControladorAutenticacao {

    private final RepositorioAuditoriaIngestao auditRepository;

    public ControladorAutenticacao(RepositorioAuditoriaIngestao auditRepository) {
        this.auditRepository = auditRepository;
    }

    @GetMapping("/profile")
    public Map<String, Object> profile(
        @AuthenticationPrincipal Jwt jwt,
        @RequestHeader(value = "X-Api-User", required = false) String apiUser
    ) {
        String email = jwt != null ? jwt.getClaimAsString("email") : (apiUser != null ? apiUser : "dev@ticketub.local");
        String name = jwt != null ? jwt.getClaimAsString("name") : "Dev User";
        String subject = jwt != null ? jwt.getSubject() : "dev-sub-123";

        // Audit the login access trace
        auditRepository.save(new RegistoAuditoriaIngestao(
            "USER_LOGIN",
            "email",
            email,
            "Utilizador " + name + " iniciou sessao / acedeu ao perfil com sucesso.",
            OffsetDateTime.now()
        ));

        return Map.of(
            "name", name,
            "email", email,
            "sub", subject
        );
    }

    @GetMapping("/public/hello")
    public String hello() {
        return "Endpoint público! Sem autenticação necessária.";
    }
}
