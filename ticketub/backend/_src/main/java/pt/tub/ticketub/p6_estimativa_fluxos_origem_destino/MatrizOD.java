package pt.tub.ticketub.p6_estimativa_fluxos_origem_destino;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "matriz_od")
public class MatrizOD {

    public static final int LIMIAR_PRIVACIDADE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "origem_stop_id", nullable = false)
    private String origemStopId;

    @Column(name = "destino_stop_id")
    private String destinoStopId;

    @Column(name = "destino_desconhecido", nullable = false)
    private boolean destinoDesconhecido = false;

    @Column(name = "route_id")
    private String routeId;

    @Column(name = "periodo", nullable = false)
    private String periodo;

    @Column(name = "data_calculo", nullable = false)
    private LocalDate dataCalculo;

    @Column(name = "volume", nullable = false)
    private int volume;

    @Column(name = "indice_confianca", nullable = false)
    private String indiceConfianca;

    @Column(name = "calculado_em", nullable = false)
    private OffsetDateTime calculadoEm;

    public MatrizOD() {}

    public MatrizOD(String origemStopId, String destinoStopId, boolean destinoDesconhecido,
             String routeId, String periodo, LocalDate dataCalculo,
             int volume, String indiceConfianca, OffsetDateTime calculadoEm) {
        this.origemStopId        = origemStopId;
        this.destinoStopId       = destinoStopId;
        this.destinoDesconhecido = destinoDesconhecido;
        this.routeId             = routeId;
        this.periodo             = periodo;
        this.dataCalculo         = dataCalculo;
        this.volume              = volume;
        this.indiceConfianca     = indiceConfianca;
        this.calculadoEm         = calculadoEm;
    }

    public Long getId()                   { return id; }
    public String getOrigemStopId()       { return origemStopId; }
    public String getDestinoStopId()      { return destinoStopId; }
    public boolean isDestinoDesconhecido(){ return destinoDesconhecido; }
    public String getRouteId()            { return routeId; }
    public String getPeriodo()            { return periodo; }
    public LocalDate getDataCalculo()     { return dataCalculo; }
    public int getVolume()                { return volume; }
    public String getIndiceConfianca()    { return indiceConfianca; }
    public OffsetDateTime getCalculadoEm(){ return calculadoEm; }
    public void setVolume(int volume)     { this.volume = volume; }
}
