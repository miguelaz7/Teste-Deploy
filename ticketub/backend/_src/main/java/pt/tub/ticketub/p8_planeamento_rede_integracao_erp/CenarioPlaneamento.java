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
public class CenarioPlaneamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador único: ex. "SC_20250418_Linha50_FreqReducao"
    @Column(name = "codigo_cenario", nullable = false, unique = true)
    private String scenarioCode;

    // Linha afectada pela simulação
    @Column(name = "route_id", nullable = false)
    private String routeId;

    // Descrição do ajuste simulado
    @Column(name = "descricao", nullable = false, length = 1000)
    private String description;

    // Período aplicável: PONTA, FORA_PONTA, DIA_COMPLETO
    @Column(name = "periodo_aplicavel", nullable = false)
    private String applicablePeriod;

    // Parâmetro de ajuste: ex. redução de 10 para 8 circulações/hora
    @Column(name = "valor_antes", precision = 10, scale = 2)
    private BigDecimal valueBefore;

    @Column(name = "valor_depois", precision = 10, scale = 2)
    private BigDecimal valueAfter;

    // Projecção de impacto calculada
    @Column(name = "ocupacao_esperada", precision = 5, scale = 2)
    private BigDecimal expectedOccupation;

    @Column(name = "receita_estimada_impacto", precision = 12, scale = 2)
    private BigDecimal estimatedImpactRevenue;

    // Nível de confiança 0-100 — aviso de risco se < 70%
    @Column(name = "nivel_confianca", nullable = false)
    private double confidenceLevel;

    // Versão dos dados de base usados (snapshot de histórico e O-D)
    @Column(name = "versao_dados_base", nullable = false)
    private String baseDataVersion;

    // Hash de integridade dos dados de base
    @Column(name = "hash_dados_base")
    private String baseDataHash;

    @Column(name = "criado_por", nullable = false)
    private String createdBy;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime createdAt;

    // Estado: RASCUNHO, PUBLICADO, ARQUIVADO
    @Column(name = "estado", nullable = false)
    private String status;

    CenarioPlaneamento() {}

    CenarioPlaneamento(String scenarioCode, String routeId, String description,
                       String applicablePeriod, BigDecimal valueBefore, BigDecimal valueAfter,
                       BigDecimal expectedOccupation, BigDecimal estimatedImpactRevenue,
                       double confidenceLevel, String baseDataVersion, String baseDataHash,
                       String createdBy, OffsetDateTime createdAt) {
        this.scenarioCode           = scenarioCode;
        this.routeId                = routeId;
        this.description            = description;
        this.applicablePeriod       = applicablePeriod;
        this.valueBefore            = valueBefore;
        this.valueAfter             = valueAfter;
        this.expectedOccupation     = expectedOccupation;
        this.estimatedImpactRevenue = estimatedImpactRevenue;
        this.confidenceLevel        = confidenceLevel;
        this.baseDataVersion        = baseDataVersion;
        this.baseDataHash           = baseDataHash;
        this.createdBy              = createdBy;
        this.createdAt              = createdAt;
        this.status                 = "RASCUNHO";
    }

    public Long getId()                                 { return id; }
    public String getScenarioCode()                     { return scenarioCode; }
    public String getRouteId()                          { return routeId; }
    public String getDescription()                      { return description; }
    public String getApplicablePeriod()                 { return applicablePeriod; }
    public BigDecimal getValueBefore()                  { return valueBefore; }
    public BigDecimal getValueAfter()                   { return valueAfter; }
    public BigDecimal getExpectedOccupation()           { return expectedOccupation; }
    public BigDecimal getEstimatedImpactRevenue()       { return estimatedImpactRevenue; }
    public double getConfidenceLevel()                  { return confidenceLevel; }
    public String getBaseDataVersion()                  { return baseDataVersion; }
    public String getBaseDataHash()                    { return baseDataHash; }
    public String getCreatedBy()                        { return createdBy; }
    public OffsetDateTime getCreatedAt()                 { return createdAt; }
    public String getStatus()                           { return status; }
    public void setStatus(String status)                { this.status = status; }
    public void setExpectedOccupation(BigDecimal v)     { this.expectedOccupation = v; }
    public void setEstimatedImpactRevenue(BigDecimal v) { this.estimatedImpactRevenue = v; }
    public void setConfidenceLevel(double v)            { this.confidenceLevel = v; }
    public void setBaseDataHash(String v)              { this.baseDataHash = v; }
}

@Repository
interface CenarioPlaneamentoRepository extends JpaRepository<CenarioPlaneamento, Long> {
    List<CenarioPlaneamento> findByRouteId(String routeId);
    List<CenarioPlaneamento> findByStatus(String status);
    boolean existsByScenarioCode(String scenarioCode);
}