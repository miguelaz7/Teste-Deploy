package pt.tub.ticketub.p1_autenticacao_controlo_acesso;

// UC01.4 – Registar auditoria de autenticação
// Persiste eventos de autenticação (sucesso/falha) na BD de auditoria.

import org.springframework.stereotype.Service;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioAuditoriaIngestao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RegistoAuditoriaIngestao;

import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ServicoAuditoria {

    private final RepositorioAuditoriaIngestao repositorioAuditoria;

    // Contador in-memory de tentativas falhadas por utilizador (sub / email)
    private final ConcurrentHashMap<String, AtomicInteger> tentativasFalhadas = new ConcurrentHashMap<>();

    public ServicoAuditoria(RepositorioAuditoriaIngestao repositorioAuditoria) {
        this.repositorioAuditoria = repositorioAuditoria;
    }

    /**
     * UC01.4 – Sucesso: regista LOGIN_SUCCESS e hora de expiração da sessão.
     */
    public void registarLoginSucesso(String sub, String email, String nome, String perfis, OffsetDateTime expiracaoSessao) {
        // Reset contador de falhas ao autenticar com sucesso
        tentativasFalhadas.remove(sub);

        String detalhe = String.format(
            "Utilizador '%s' (%s) autenticado com sucesso. Perfis atribuídos: [%s]. Sessão expira às %s.",
            nome, email, perfis, expiracaoSessao != null ? expiracaoSessao.toString() : "N/D"
        );

        repositorioAuditoria.save(new RegistoAuditoriaIngestao(
            "LOGIN_SUCCESS",
            "sub",
            sub,
            detalhe,
            OffsetDateTime.now()
        ));
    }

    /**
     * UC01.4 – Falha: regista LOGIN_FAILED_[MOTIVO] sem expor detalhes técnicos.
     * Incrementa contador de falhas e alerta se ultrapassar threshold.
     */
    public void registarLoginFalha(String identificador, String motivo, String detalheInterno) {
        AtomicInteger contador = tentativasFalhadas.computeIfAbsent(identificador, k -> new AtomicInteger(0));
        int tentativas = contador.incrementAndGet();

        String detalhe = String.format(
            "Tentativa de acesso falhada [%s]. Identificador: '%s'. Tentativas consecutivas: %d. Causa interna (não exposta ao utilizador): %s",
            motivo, identificador, tentativas, detalheInterno
        );

        repositorioAuditoria.save(new RegistoAuditoriaIngestao(
            "LOGIN_FAILED_" + motivo.toUpperCase().replace(" ", "_"),
            "identificador",
            identificador,
            detalhe,
            OffsetDateTime.now()
        ));

        // FA2: Múltiplas falhas – registar alerta adicional (>=3 tentativas)
        if (tentativas >= 3) {
            repositorioAuditoria.save(new RegistoAuditoriaIngestao(
                "LOGIN_ALERTA_MULTIPLAS_FALHAS",
                "identificador",
                identificador,
                String.format("ALERTA: %d tentativas de acesso falhadas consecutivas para '%s'. Revisão administrativa recomendada.", tentativas, identificador),
                OffsetDateTime.now()
            ));
        }
    }

    /**
     * UC01.4 – FA4: Utilizador sem perfil mapeado.
     */
    public void registarSemPerfil(String sub, String email) {
        repositorioAuditoria.save(new RegistoAuditoriaIngestao(
            "LOGIN_FAILED_SEM_PERFIL",
            "sub",
            sub,
            String.format("Utilizador '%s' autenticado no IdP mas sem perfil interno mapeado. Acesso bloqueado. Notificação enviada ao administrador.", email),
            OffsetDateTime.now()
        ));
    }

    /**
     * UC01.4 – FA1: IdP indisponível.
     */
    public void registarIdpIndisponivel(String detalhe) {
        repositorioAuditoria.save(new RegistoAuditoriaIngestao(
            "LOGIN_FAILED_IDP_INDISPONIVEL",
            "sistema",
            "auth0",
            "Fornecedor de identidade indisponível. " + detalhe,
            OffsetDateTime.now()
        ));
    }
}
