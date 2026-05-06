package pt.tub.ticketub.p5_analise_operacional_tempo_real;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historico_consolidado")
public class HistoricoConsolidado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "perfil_tarifario")
    private String perfilTarifario;

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

    public HistoricoConsolidado() {}

    public HistoricoConsolidado(String routeId, String perfilTarifario, String granularidade,
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

    public Long getId()                     { return id; }
    public String getRouteId()              { return routeId; }
    public String getPerfilTarifario()      { return perfilTarifario; }
    public String getGranularidade()        { return granularidade; }
    public LocalDate getPeriodoInicio()     { return periodoInicio; }
    public LocalDate getPeriodoFim()        { return periodoFim; }
    public long getTotalValidacoes()        { return totalValidacoes; }
    public BigDecimal getReceitaEstimada()  { return receitaEstimada; }
    public double getTaxaAnomalias()        { return taxaAnomalias; }
    public OffsetDateTime getActualizadoEm(){ return actualizadoEm; }
    public void setTotalValidacoes(long v)  { this.totalValidacoes = v; }
    public void setReceitaEstimada(BigDecimal v) { this.receitaEstimada = v; }
    public void setTaxaAnomalias(double v)  { this.taxaAnomalias = v; }
    public void setActualizadoEm(OffsetDateTime t) { this.actualizadoEm = t; }
}
