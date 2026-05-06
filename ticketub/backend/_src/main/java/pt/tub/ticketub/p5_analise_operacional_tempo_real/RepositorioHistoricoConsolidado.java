package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O010.1.d – Repositório de Histórico Consolidado
// Agregados históricos por linha, veículo, perfil e período: validações,
// receita estimada, taxa de anomalias e ocupação.
// Actualizado continuamente. Suporte mínimo de 7 anos.
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "historico_consolidado")
class HistoricoConsolidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "perfil_tarifario")
    private String perfilTarifario;

    // Granularidade: DIA, SEMANA, MES
    @Column(name = "granularidade", nullable = false)
    private String granularidade;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Column(name = "total_validacoes", nullable = false)
    private long totalValidacoes;

    @Column(name = "receita_estimada", precision = 12, scale = 2)
    private BigDecimal receitaEstimada;

    @Column(name = "taxa_anomalias")
    private double taxaAnomalias;

    @Column(name = "actualizado_em", nullable = false)
    private OffsetDateTime actualizadoEm;

    HistoricoConsolidado() {}

    HistoricoConsolidado(String routeId, String perfilTarifario, String granularidade,
                         LocalDate periodoInicio, LocalDate periodoFim,
                         long totalValidacoes, BigDecimal receitaEstimada,
                         double taxaAnomalias, OffsetDateTime actualizadoEm) {
        this.routeId          = routeId;
        this.perfilTarifario  = perfilTarifario;
        this.granularidade    = granularidade;
        this.periodoInicio    = periodoInicio;
        this.periodoFim       = periodoFim;
        this.totalValidacoes  = totalValidacoes;
        this.receitaEstimada  = receitaEstimada;
        this.taxaAnomalias    = taxaAnomalias;
        this.actualizadoEm    = actualizadoEm;
    }

    Long getId()                     { return id; }
    String getRouteId()              { return routeId; }
    String getPerfilTarifario()      { return perfilTarifario; }
    String getGranularidade()        { return granularidade; }
    LocalDate getPeriodoInicio()     { return periodoInicio; }
    LocalDate getPeriodoFim()        { return periodoFim; }
    long getTotalValidacoes()        { return totalValidacoes; }
    BigDecimal getReceitaEstimada()  { return receitaEstimada; }
    double getTaxaAnomalias()        { return taxaAnomalias; }
    OffsetDateTime getActualizadoEm(){ return actualizadoEm; }
    void setTotalValidacoes(long v)  { this.totalValidacoes = v; }
    void setReceitaEstimada(BigDecimal v) { this.receitaEstimada = v; }
    void setTaxaAnomalias(double v)  { this.taxaAnomalias = v; }
    void setActualizadoEm(OffsetDateTime t) { this.actualizadoEm = t; }
}

@Repository
interface HistoricoConsolidadoRepository extends JpaRepository<HistoricoConsolidado, Long> {
    List<HistoricoConsolidado> findByRouteId(String routeId);
    List<HistoricoConsolidado> findByGranularidade(String granularidade);
    Optional<HistoricoConsolidado> findByRouteIdAndPerfilTarifarioAndGranularidadeAndPeriodoInicio(
        String routeId, String perfilTarifario,
        String granularidade, LocalDate periodoInicio);
}
