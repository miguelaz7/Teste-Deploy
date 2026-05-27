package pt.tub.ticketub.p8_planeamento_rede_integracao_erp;

// =============================================================================
// O011.2.d – Repositório de Dados Financeiros para ERP
// Dados financeiros formatados para o ERP: receita por linha e título,
// validações por canal e anomalias com impacto financeiro.
// Cada exportação tem versão, data, utilizador e hash de integridade.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "dados_financeiros_erp")
public class DadosFinanceirosERP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Linha de transporte
    @Column(name = "route_id", nullable = false)
    private String routeId;

    // Tipo de título
    @Column(name = "tipo_titulo", nullable = false)
    private String ticketType;

    // Período de referência
    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodStart;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodEnd;

    // Métricas financeiras
    @Column(name = "total_validacoes", nullable = false)
    private long totalValidations;

    @Column(name = "receita_estimada", precision = 12, scale = 2)
    private BigDecimal estimatedRevenue;

    @Column(name = "receita_por_km", precision = 10, scale = 4)
    private BigDecimal revenuePerKm;

    @Column(name = "receita_por_passageiro", precision = 10, scale = 4)
    private BigDecimal revenuePerPassenger;

    @Column(name = "taxa_anomalias", precision = 5, scale = 2)
    private BigDecimal anomalyRate;

    @Column(name = "impacto_financeiro_anomalias", precision = 10, scale = 2)
    private BigDecimal financialImpactAnomalies;

    // Versioning da exportação
    @Column(name = "versao_exportacao", nullable = false)
    private String exportVersion;

    @Column(name = "hash_integridade", nullable = false)
    private String integrityHash;

    // Estado de envio ao ERP: PENDENTE, ENVIADO, FALHA, VALIDACAO_MANUAL
    @Column(name = "estado_erp", nullable = false)
    private String erpStatus;

    // Campo marcado para validação manual (ex: linhas com anomalias)
    @Column(name = "requer_validacao_manual", nullable = false)
    private boolean requiresManualValidation;

    @Column(name = "gerado_por", nullable = false)
    private String generatedBy;

    @Column(name = "gerado_em", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "enviado_em")
    private OffsetDateTime sentAt;

    // Payload formatado para o ERP (CSV/XML)
    @Lob
    @Column(name = "payload_erp", columnDefinition = "LONGTEXT")
    private String erpPayload;

    DadosFinanceirosERP() {}

    DadosFinanceirosERP(String routeId, String ticketType, LocalDate periodStart,
                        LocalDate periodEnd, long totalValidations, BigDecimal estimatedRevenue,
                        BigDecimal revenuePerKm, BigDecimal revenuePerPassenger,
                        BigDecimal anomalyRate, BigDecimal financialImpactAnomalies,
                        String exportVersion, String integrityHash,
                        boolean requiresManualValidation, String generatedBy,
                        OffsetDateTime generatedAt, String erpPayload) {
        this.routeId                    = routeId;
        this.ticketType                 = ticketType;
        this.periodStart                = periodStart;
        this.periodEnd                  = periodEnd;
        this.totalValidations            = totalValidations;
        this.estimatedRevenue            = estimatedRevenue;
        this.revenuePerKm               = revenuePerKm;
        this.revenuePerPassenger       = revenuePerPassenger;
        this.anomalyRate                = anomalyRate;
        this.financialImpactAnomalies = financialImpactAnomalies;
        this.exportVersion              = exportVersion;
        this.integrityHash              = integrityHash;
        this.erpStatus                  = "PENDENTE";
        this.requiresManualValidation  = requiresManualValidation;
        this.generatedBy                = generatedBy;
        this.generatedAt                 = generatedAt;
        this.erpPayload                 = erpPayload;
    }

    public Long getId()                                 { return id; }
    public String getRouteId()                          { return routeId; }
    public String getTicketType()                       { return ticketType; }
    public LocalDate getPeriodStart()                   { return periodStart; }
    public LocalDate getPeriodEnd()                     { return periodEnd; }
    public long getTotalValidations()                   { return totalValidations; }
    public BigDecimal getEstimatedRevenue()             { return estimatedRevenue; }
    public BigDecimal getRevenuePerKm()                 { return revenuePerKm; }
    public BigDecimal getRevenuePerPassenger()         { return revenuePerPassenger; }
    public BigDecimal getAnomalyRate()                  { return anomalyRate; }
    public BigDecimal getFinancialImpactAnomalies()     { return financialImpactAnomalies; }
    public String getExportVersion()                    { return exportVersion; }
    public String getIntegrityHash()                    { return integrityHash; }
    public String getErpStatus()                        { return erpStatus; }
    public void setErpStatus(String erpStatus)          { this.erpStatus = erpStatus; }
    public boolean isRequiresManualValidation()         { return requiresManualValidation; }
    public String getGeneratedBy()                      { return generatedBy; }
    public OffsetDateTime getGeneratedAt()              { return generatedAt; }
    public OffsetDateTime getSentAt()                   { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt)        { this.sentAt = sentAt; }
    public String getErpPayload()                       { return erpPayload; }
}

@Repository
interface DadosFinanceirosERPRepository extends JpaRepository<DadosFinanceirosERP, Long> {
    List<DadosFinanceirosERP> findByErpStatus(String erpStatus);
    List<DadosFinanceirosERP> findByRouteIdAndPeriodStartBetween(
        String routeId, LocalDate inicio, LocalDate fim);
    Optional<DadosFinanceirosERP> findByExportVersion(String exportVersion);
}