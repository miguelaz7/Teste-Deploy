package pt.tub.ticketub.p2_ingestao_processamento_dados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.annotation.PostConstruct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// =============================================================================
// O0.2.1.d – Repositório de Regras de Validação
// Guarda regras de validação, limites temporais e motivos de rejeição.
// =============================================================================

@Entity
@Table(name = "validation_rules")
class RegraValidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false, unique = true)
    private String ruleCode;

    @Column(name = "campo", nullable = false)
    private String campo;

    @Column(name = "tipo_regra", nullable = false)
    private String tipoRegra;

    @Column(name = "valor_limite", length = 500)
    private String valorLimite;

    @Column(name = "motivo_rejeicao", nullable = false, length = 500)
    private String motivoRejeicao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    RegraValidacao() {}

    RegraValidacao(String ruleCode, String campo, String tipoRegra,
                   String valorLimite, String motivoRejeicao) {
        this.ruleCode = ruleCode;
        this.campo = campo;
        this.tipoRegra = tipoRegra;
        this.valorLimite = valorLimite;
        this.motivoRejeicao = motivoRejeicao;
        this.ativo = true;
    }

    Long getId() { return id; }
    String getRuleCode() { return ruleCode; }
    String getCampo() { return campo; }
    String getTipoRegra() { return tipoRegra; }
    String getValorLimite() { return valorLimite; }
    String getMotivoRejeicao() { return motivoRejeicao; }
    boolean isAtivo() { return ativo; }
}

@Repository
public interface RepositorioRegrasValidacao extends JpaRepository<RegraValidacao, Long> {
    List<RegraValidacao> findByAtivoTrue();
    Optional<RegraValidacao> findByRuleCode(String ruleCode);
}

// Povoa a tabela ao arrancar, se ainda estiver vazia
@Component
class SemeadorRegrasValidacao {

    private final RepositorioRegrasValidacao repository;

    SemeadorRegrasValidacao(RepositorioRegrasValidacao repository) {
        this.repository = repository;
    }

    @PostConstruct
    void populate() {
        if (repository.count() > 0) return;

        repository.save(new RegraValidacao(
            "OBRIGATORIO_transactionDateTime", "transactionDateTime",
            "OBRIGATORIO", null, "transactionDateTime em falta"));

        repository.save(new RegraValidacao(
            "FORMATO_ISO8601_transactionDateTime", "transactionDateTime",
            "FORMATO_ISO8601", null, "transactionDateTime invalido - formato ISO-8601 esperado"));

        repository.save(new RegraValidacao(
            "LIMITE_FUTURO_transactionDateTime", "transactionDateTime",
            "LIMITE_FUTURO", "5", "timestamp no futuro - tolerancia maxima de 5 minutos"));

        repository.save(new RegraValidacao(
            "LIMITE_PASSADO_transactionDateTime", "transactionDateTime",
            "LIMITE_PASSADO", "90", "timestamp demasiado antigo - limite maximo de 90 dias"));

        repository.save(new RegraValidacao(
            "OBRIGATORIO_route_id", "route_id",
            "OBRIGATORIO", null, "route_id em falta"));

        repository.save(new RegraValidacao(
            "CATALOGO_route_id", "route_id",
            "CATALOGO", null, "route_id nao encontrado no catalogo de linhas"));

        repository.save(new RegraValidacao(
            "OBRIGATORIO_ticketTypeCode", "ticketTypeCode",
            "OBRIGATORIO", null, "ticketTypeCode em falta"));

        repository.save(new RegraValidacao(
            "LISTA_APROVADA_ticketTypeCode", "ticketTypeCode",
            "LISTA_APROVADA",
            "PASSE_ESTUDANTE,ESTUDANTE,AVULSO,SINGLE_TICKET,MENSAL,MONTHLY_PASS,PASSE_SENIOR,SENIOR_PASS",
            "ticketTypeCode fora das categorias validas"));

        repository.save(new RegraValidacao(
            "OBRIGATORIO_trip_id", "trip_id",
            "OBRIGATORIO", null, "trip_id em falta"));

        repository.save(new RegraValidacao(
            "CATALOGO_trip_id", "trip_id",
            "CATALOGO", null, "trip_id nao encontrado no catalogo"));

        repository.save(new RegraValidacao(
            "COERENCIA_route_trip", "trip_id",
            "COERENCIA", null, "trip_id nao pertence ao route_id indicado"));

        repository.save(new RegraValidacao(
            "OBRIGATORIO_result", "result",
            "OBRIGATORIO", null, "result em falta"));

        repository.save(new RegraValidacao(
            "CATALOGO_originStopId", "originStopId",
            "CATALOGO", null, "originStopId nao encontrado no catalogo de paragens"));

        repository.save(new RegraValidacao(
            "COERENCIA_trip_stop", "originStopId",
            "COERENCIA", null, "originStopId nao pertence ao trip_id indicado"));

        repository.save(new RegraValidacao(
            "FORMATO_DECIMAL_fareForAdult", "fareForAdult",
            "FORMATO_DECIMAL", null, "fareForAdult invalido - tipo decimal esperado"));
    }
}
