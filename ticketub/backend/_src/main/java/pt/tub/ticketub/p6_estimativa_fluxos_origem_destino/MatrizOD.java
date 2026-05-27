package pt.tub.ticketub.p6_estimativa_fluxos_origem_destino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// =============================================================================
// O0.8.1.d – Repositório da Matriz Origem-Destino (UC08.1)
// Armazena a matriz O-D diária agregada por período com indicador de
// confiança. Pares com destino desconhecido marcados explicitamente.
// Agregados acima do limiar mínimo de privacidade.
// =============================================================================

@Entity
@Table(name = "matriz_od")
public class MatrizOD {

    // Limiar mínimo de privacidade — pares com menos ocorrências não são exportados
    static final int LIMIAR_PRIVACIDADE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Paragem de origem do par O-D
    @Column(name = "origem_stop_id", nullable = false)
    private String originStopId;

    // Paragem de destino estimada — null se desconhecido
    @Column(name = "destino_stop_id")
    private String destinationStopId;

    // Destino desconhecido quando não foi possível estimar (marcado explicitamente)
    @Column(name = "destino_desconhecido", nullable = false)
    private boolean destinationUnknown = false;

    // Linha associada ao par O-D
    @Column(name = "route_id")
    private String routeId;

    // Período do dia: PONTA_MANHA, PONTA_TARDE, VAZIO, FIM_SEMANA
    @Column(name = "periodo", nullable = false)
    private String period;

    // Data a que este par O-D pertence
    @Column(name = "data_calculo", nullable = false)
    private LocalDate calculationDate;

    // Número de ocorrências deste par neste período e data
    @Column(name = "volume", nullable = false)
    private int volume;

    // Índice de confiança: ALTO (baseado em validação seguinte),
    // MEDIO (baseado em contagem), BAIXO (terminus como fallback)
    @Column(name = "indice_confianca")
    private String confidenceIndex;

    @Column(name = "calculado_em", nullable = false)
    private OffsetDateTime calculatedAt;

    MatrizOD() {}

    MatrizOD(String originStopId, String destinationStopId, boolean destinationUnknown,
             String routeId, String period, LocalDate calculationDate,
             int volume, String confidenceIndex, OffsetDateTime calculatedAt) {
        this.originStopId        = originStopId;
        this.destinationStopId   = destinationStopId;
        this.destinationUnknown  = destinationUnknown;
        this.routeId             = routeId;
        this.period              = period;
        this.calculationDate     = calculationDate;
        this.volume              = volume;
        this.confidenceIndex     = confidenceIndex;
        this.calculatedAt         = calculatedAt;
    }

    public Long getId()                      { return id; }
    public String getOriginStopId()          { return originStopId; }
    public String getDestinationStopId()     { return destinationStopId; }
    public boolean isDestinationUnknown()    { return destinationUnknown; }
    public String getRouteId()               { return routeId; }
    public String getPeriod()                { return period; }
    public LocalDate getCalculationDate()    { return calculationDate; }
    public int getVolume()                   { return volume; }
    public String getConfidenceIndex()       { return confidenceIndex; }
    public OffsetDateTime getCalculatedAt()  { return calculatedAt; }
    public void setVolume(int volume)        { this.volume = volume; }
}
