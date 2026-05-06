package pt.tub.ticketub.p8_planeamento_rede_integracao_erp;

// =============================================================================
// O011.1.d – Repositório de Cenários de Planeamento
// Parâmetros, projecções, nível de confiança e versão dos dados de base
// de cada cenário, com identificação do utilizador e data.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "cenarios_planeamento")
class CenarioPlaneamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador único: ex. "SC_20250418_Linha50_FreqReducao"
    @Column(name = "codigo_cenario", nullable = false, unique = true)
    private String codigoCenario;

    // Linha afectada pela simulação
    @Column(name = "route_id", nullable = false)
    private String routeId;

    // Descrição do ajuste simulado
    @Column(name = "descricao", nullable = false, length = 1000)
    private String descricao;

    // Período aplicável: PONTA, FORA_PONTA, DIA_COMPLETO
    @Column(name = "periodo_aplicavel", nullable = false)
    private String periodoAplicavel;

    // Parâmetro de ajuste: ex. redução de 10 para 8 circulações/hora
    @Column(name = "valor_antes", precision = 10, scale = 2)
    private BigDecimal valorAntes;

    @Column(name = "valor_depois", precision = 10, scale = 2)
    private BigDecimal valorDepois;

    // Projecção de impacto calculada
    @Column(name = "ocupacao_esperada", precision = 5, scale = 2)
    private BigDecimal ocupacaoEsperada;

    @Column(name = "receita_estimada_impacto", precision = 12, scale = 2)
    private BigDecimal receitaEstimadaImpacto;

    // Nível de confiança 0-100 — aviso de risco se < 70%
    @Column(name = "nivel_confianca", nullable = false)
    private double nivelConfianca;

    // Versão dos dados de base usados (snapshot de histórico e O-D)
    @Column(name = "versao_dados_base", nullable = false)
    private String versaoDadosBase;

    // Hash de integridade dos dados de base
    @Column(name = "hash_dados_base")
    private String hashDadosBase;

    @Column(name = "criado_por", nullable = false)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    // Estado: RASCUNHO, PUBLICADO, ARQUIVADO
    @Column(name = "estado", nullable = false)
    private String estado;

    CenarioPlaneamento() {}

    CenarioPlaneamento(String codigoCenario, String routeId, String descricao,
                       String periodoAplicavel, BigDecimal valorAntes, BigDecimal valorDepois,
                       BigDecimal ocupacaoEsperada, BigDecimal receitaEstimadaImpacto,
                       double nivelConfianca, String versaoDadosBase, String hashDadosBase,
                       String criadoPor, OffsetDateTime criadoEm) {
        this.codigoCenario         = codigoCenario;
        this.routeId               = routeId;
        this.descricao             = descricao;
        this.periodoAplicavel      = periodoAplicavel;
        this.valorAntes            = valorAntes;
        this.valorDepois           = valorDepois;
        this.ocupacaoEsperada      = ocupacaoEsperada;
        this.receitaEstimadaImpacto = receitaEstimadaImpacto;
        this.nivelConfianca        = nivelConfianca;
        this.versaoDadosBase       = versaoDadosBase;
        this.hashDadosBase         = hashDadosBase;
        this.criadoPor             = criadoPor;
        this.criadoEm              = criadoEm;
        this.estado                = "RASCUNHO";
    }

    Long getId()                        { return id; }
    String getCodigoCenario()           { return codigoCenario; }
    String getRouteId()                 { return routeId; }
    String getDescricao()               { return descricao; }
    String getPeriodoAplicavel()        { return periodoAplicavel; }
    BigDecimal getValorAntes()          { return valorAntes; }
    BigDecimal getValorDepois()         { return valorDepois; }
    BigDecimal getOcupacaoEsperada()    { return ocupacaoEsperada; }
    BigDecimal getReceitaEstimadaImpacto() { return receitaEstimadaImpacto; }
    double getNivelConfianca()          { return nivelConfianca; }
    String getVersaoDadosBase()         { return versaoDadosBase; }
    String getHashDadosBase()           { return hashDadosBase; }
    String getCriadoPor()               { return criadoPor; }
    OffsetDateTime getCriadoEm()        { return criadoEm; }
    String getEstado()                  { return estado; }
    void setEstado(String estado)       { this.estado = estado; }
    void setOcupacaoEsperada(BigDecimal v)       { this.ocupacaoEsperada = v; }
    void setReceitaEstimadaImpacto(BigDecimal v) { this.receitaEstimadaImpacto = v; }
    void setNivelConfianca(double v)    { this.nivelConfianca = v; }
    void setHashDadosBase(String v)     { this.hashDadosBase = v; }
}

@Repository
interface CenarioPlaneamentoRepository extends JpaRepository<CenarioPlaneamento, Long> {
    List<CenarioPlaneamento> findByRouteId(String routeId);
    List<CenarioPlaneamento> findByEstado(String estado);
    boolean existsByCodigoCenario(String codigoCenario);
}
