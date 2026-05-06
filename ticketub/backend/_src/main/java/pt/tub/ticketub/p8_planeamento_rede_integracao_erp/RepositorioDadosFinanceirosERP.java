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
class DadosFinanceirosERP {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Linha de transporte
    @Column(name = "route_id", nullable = false)
    private String routeId;

    // Tipo de título
    @Column(name = "tipo_titulo", nullable = false)
    private String tipoTitulo;

    // Período de referência
    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    // Métricas financeiras
    @Column(name = "total_validacoes", nullable = false)
    private long totalValidacoes;

    @Column(name = "receita_estimada", precision = 12, scale = 2)
    private BigDecimal receitaEstimada;

    @Column(name = "receita_por_km", precision = 10, scale = 4)
    private BigDecimal receitaPorKm;

    @Column(name = "receita_por_passageiro", precision = 10, scale = 4)
    private BigDecimal receitaPorPassageiro;

    @Column(name = "taxa_anomalias", precision = 5, scale = 2)
    private BigDecimal taxaAnomalias;

    @Column(name = "impacto_financeiro_anomalias", precision = 10, scale = 2)
    private BigDecimal impactoFinanceiroAnomalias;

    // Versioning da exportação
    @Column(name = "versao_exportacao", nullable = false)
    private String versaoExportacao;

    @Column(name = "hash_integridade", nullable = false)
    private String hashIntegridade;

    // Estado de envio ao ERP: PENDENTE, ENVIADO, FALHA, VALIDACAO_MANUAL
    @Column(name = "estado_erp", nullable = false)
    private String estadoERP;

    // Campo marcado para validação manual (ex: linhas com anomalias)
    @Column(name = "requer_validacao_manual", nullable = false)
    private boolean requerValidacaoManual;

    @Column(name = "gerado_por", nullable = false)
    private String geradoPor;

    @Column(name = "gerado_em", nullable = false)
    private OffsetDateTime geradoEm;

    @Column(name = "enviado_em")
    private OffsetDateTime enviadoEm;

    // Payload formatado para o ERP (CSV/XML)
    @Lob
    @Column(name = "payload_erp", columnDefinition = "LONGTEXT")
    private String payloadERP;

    DadosFinanceirosERP() {}

    DadosFinanceirosERP(String routeId, String tipoTitulo, LocalDate periodoInicio,
                        LocalDate periodoFim, long totalValidacoes, BigDecimal receitaEstimada,
                        BigDecimal receitaPorKm, BigDecimal receitaPorPassageiro,
                        BigDecimal taxaAnomalias, BigDecimal impactoFinanceiroAnomalias,
                        String versaoExportacao, String hashIntegridade,
                        boolean requerValidacaoManual, String geradoPor,
                        OffsetDateTime geradoEm, String payloadERP) {
        this.routeId                    = routeId;
        this.tipoTitulo                 = tipoTitulo;
        this.periodoInicio              = periodoInicio;
        this.periodoFim                 = periodoFim;
        this.totalValidacoes            = totalValidacoes;
        this.receitaEstimada            = receitaEstimada;
        this.receitaPorKm               = receitaPorKm;
        this.receitaPorPassageiro       = receitaPorPassageiro;
        this.taxaAnomalias              = taxaAnomalias;
        this.impactoFinanceiroAnomalias = impactoFinanceiroAnomalias;
        this.versaoExportacao           = versaoExportacao;
        this.hashIntegridade            = hashIntegridade;
        this.estadoERP                  = "PENDENTE";
        this.requerValidacaoManual      = requerValidacaoManual;
        this.geradoPor                  = geradoPor;
        this.geradoEm                   = geradoEm;
        this.payloadERP                 = payloadERP;
    }

    Long getId()                            { return id; }
    String getRouteId()                     { return routeId; }
    String getTipoTitulo()                  { return tipoTitulo; }
    LocalDate getPeriodoInicio()            { return periodoInicio; }
    LocalDate getPeriodoFim()               { return periodoFim; }
    long getTotalValidacoes()               { return totalValidacoes; }
    BigDecimal getReceitaEstimada()         { return receitaEstimada; }
    BigDecimal getReceitaPorKm()            { return receitaPorKm; }
    BigDecimal getReceitaPorPassageiro()    { return receitaPorPassageiro; }
    BigDecimal getTaxaAnomalias()           { return taxaAnomalias; }
    BigDecimal getImpactoFinanceiroAnomalias() { return impactoFinanceiroAnomalias; }
    String getVersaoExportacao()            { return versaoExportacao; }
    String getHashIntegridade()             { return hashIntegridade; }
    String getEstadoERP()                   { return estadoERP; }
    void setEstadoERP(String estado)        { this.estadoERP = estado; }
    boolean isRequerValidacaoManual()       { return requerValidacaoManual; }
    String getGeradoPor()                   { return geradoPor; }
    OffsetDateTime getGeradoEm()            { return geradoEm; }
    OffsetDateTime getEnviadoEm()           { return enviadoEm; }
    void setEnviadoEm(OffsetDateTime t)     { this.enviadoEm = t; }
    String getPayloadERP()                  { return payloadERP; }
}

@Repository
interface DadosFinanceirosERPRepository extends JpaRepository<DadosFinanceirosERP, Long> {
    List<DadosFinanceirosERP> findByEstadoERP(String estadoERP);
    List<DadosFinanceirosERP> findByRouteIdAndPeriodoInicioBetween(
        String routeId, LocalDate inicio, LocalDate fim);
    Optional<DadosFinanceirosERP> findByVersaoExportacao(String versaoExportacao);
}
