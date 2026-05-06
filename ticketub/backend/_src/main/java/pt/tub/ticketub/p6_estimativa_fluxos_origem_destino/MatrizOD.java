package pt.tub.ticketub.p6_estimativa_fluxos_origem_destino;

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

// =============================================================================
// O0.8.1.d – Repositório da Matriz Origem-Destino (UC08.1)
// Armazena a matriz O-D diária agregada por período com indicador de
// confiança. Pares com destino desconhecido marcados explicitamente.
// Agregados acima do limiar mínimo de privacidade.
// =============================================================================

@Entity
@Table(name = "matriz_od")
class MatrizOD {

    // Limiar mínimo de privacidade — pares com menos ocorrências não são exportados
    static final int LIMIAR_PRIVACIDADE = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Paragem de origem do par O-D
    @Column(name = "origem_stop_id", nullable = false)
    private String origemStopId;

    // Paragem de destino estimada — null se desconhecido
    @Column(name = "destino_stop_id")
    private String destinoStopId;

    // Destino desconhecido quando não foi possível estimar (marcado explicitamente)
    @Column(name = "destino_desconhecido", nullable = false)
    private boolean destinoDesconhecido = false;

    // Linha associada ao par O-D
    @Column(name = "route_id")
    private String routeId;

    // Período do dia: PONTA_MANHA, PONTA_TARDE, VAZIO, FIM_SEMANA
    @Column(name = "periodo", nullable = false)
    private String periodo;

    // Data a que este par O-D pertence
    @Column(name = "data_calculo", nullable = false)
    private LocalDate dataCalculo;

    // Número de ocorrências deste par neste período e data
    @Column(name = "volume", nullable = false)
    private int volume;

    // Índice de confiança: ALTO (baseado em validação seguinte),
    // MEDIO (baseado em contagem), BAIXO (terminus como fallback)
    @Column(name = "indice_confianca", nullable = false)
    private String indiceConfianca;

    @Column(name = "calculado_em", nullable = false)
    private OffsetDateTime calculadoEm;

    MatrizOD() {}

    MatrizOD(String origemStopId, String destinoStopId, boolean destinoDesconhecido,
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

    Long getId()                   { return id; }
    String getOrigemStopId()       { return origemStopId; }
    String getDestinoStopId()      { return destinoStopId; }
    boolean isDestinoDesconhecido(){ return destinoDesconhecido; }
    String getRouteId()            { return routeId; }
    String getPeriodo()            { return periodo; }
    LocalDate getDataCalculo()     { return dataCalculo; }
    int getVolume()                { return volume; }
    String getIndiceConfianca()    { return indiceConfianca; }
    OffsetDateTime getCalculadoEm(){ return calculadoEm; }
    void setVolume(int volume)     { this.volume = volume; }
}

@Repository
interface MatrizODRepository extends JpaRepository<MatrizOD, Long> {

    // Busca todos os pares de uma data específica
    List<MatrizOD> findByDataCalculo(LocalDate dataCalculo);

    // Busca pares acima do limiar mínimo de privacidade (para exportação)
    List<MatrizOD> findByDataCalculoAndVolumeGreaterThanEqual(LocalDate dataCalculo, int limiar);

    // Verifica se já foi calculada a matriz para uma data
    boolean existsByDataCalculo(LocalDate dataCalculo);
}
