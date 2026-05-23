package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O0.7.2.d – Repositório de Desvios Operacionais
// Resultados da análise de desvio por linha e período: validações realizadas
// vs. esperadas e desvio percentual.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "desvios_operacionais")
public class DesvioOperacional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_id", nullable = false)
    private String routeId;

    @Column(name = "data_calculo", nullable = false)
    private LocalDate dataCalculo;

    // Período: PONTA_MANHA, PONTA_TARDE, VAZIO, DIA_COMPLETO
    @Column(name = "periodo", nullable = false)
    private String periodo;

    @Column(name = "validacoes_realizadas", nullable = false)
    private long validacoesRealizadas;

    // Média histórica das últimas 4 semanas (mesma linha, mesmo período)
    @Column(name = "media_historica", nullable = false)
    private double mediaHistorica;

    @Column(name = "desvio_percentual", nullable = false)
    private double desvioPercentual;

    // Se desvio > limiar configurável → alerta
    @Column(name = "critico", nullable = false)
    private boolean critico;

    @Column(name = "calculado_em", nullable = false)
    private OffsetDateTime calculadoEm;

    DesvioOperacional() {}

    DesvioOperacional(String routeId, LocalDate dataCalculo, String periodo,
                      long validacoesRealizadas, double mediaHistorica,
                      double desvioPercentual, boolean critico,
                      OffsetDateTime calculadoEm) {
        this.routeId              = routeId;
        this.dataCalculo          = dataCalculo;
        this.periodo              = periodo;
        this.validacoesRealizadas = validacoesRealizadas;
        this.mediaHistorica       = mediaHistorica;
        this.desvioPercentual     = desvioPercentual;
        this.critico              = critico;
        this.calculadoEm          = calculadoEm;
    }

    public Long getId()                    { return id; }
    public String getRouteId()             { return routeId; }
    public LocalDate getDataCalculo()      { return dataCalculo; }
    public String getPeriodo()             { return periodo; }
    public long getValidacoesRealizadas()  { return validacoesRealizadas; }
    public double getMediaHistorica()      { return mediaHistorica; }
    public double getDesvioPercentual()    { return desvioPercentual; }
    public boolean isCritico()             { return critico; }
    public OffsetDateTime getCalculadoEm() { return calculadoEm; }
}

@Repository
interface DesvioOperacionalRepository extends JpaRepository<DesvioOperacional, Long> {
    List<DesvioOperacional> findByDataCalculo(LocalDate dataCalculo);
    List<DesvioOperacional> findByCriticoTrue();
    List<DesvioOperacional> findByRouteIdAndDataCalculoBetween(
        String routeId, LocalDate inicio, LocalDate fim);
}