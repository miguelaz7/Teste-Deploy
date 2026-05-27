package pt.tub.ticketub.p5_analise_operacional_tempo_real;

// =============================================================================
// O0.5.1.d – Repositório Analítico de Procura
// Indicadores de procura em tempo real: agregações por horário, linha e
// zona/paragem. Actualizado incrementalmente a cada nova ingestão.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "agregados_procura")
public class AgregadoProcura {

    // Perspectiva: HORARIO, LINHA, ZONA_PARAGEM
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "perspectiva", nullable = false)
    private String perspectiva;

    // Chave de agregação: hora (0-23), routeId ou stopId
    @Column(name = "chave", nullable = false)
    private String chave;

    @Column(name = "total_validacoes", nullable = false)
    private long totalValidacoes;

    @Column(name = "total_invalidas", nullable = false)
    private long totalInvalidas;

    @Column(name = "perfil_estudante", nullable = false)
    private long perfilEstudante;

    @Column(name = "perfil_senior", nullable = false)
    private long perfilSenior;

    @Column(name = "perfil_normal", nullable = false)
    private long perfilNormal;

    @Column(name = "actualizado_em", nullable = false)
    private OffsetDateTime actualizadoEm;

    AgregadoProcura() {}

    AgregadoProcura(String perspectiva, String chave, long totalValidacoes,
                    long totalInvalidas, long perfilEstudante, long perfilSenior,
                    long perfilNormal, OffsetDateTime actualizadoEm) {
        this.perspectiva     = perspectiva;
        this.chave           = chave;
        this.totalValidacoes = totalValidacoes;
        this.totalInvalidas  = totalInvalidas;
        this.perfilEstudante = perfilEstudante;
        this.perfilSenior    = perfilSenior;
        this.perfilNormal    = perfilNormal;
        this.actualizadoEm   = actualizadoEm;
    }

    Long getId()                  { return id; }
    public String getPerspectiva()       { return perspectiva; }
    public String getChave()             { return chave; }
    public long getTotalValidacoes()     { return totalValidacoes; }
    public long getTotalInvalidas()      { return totalInvalidas; }
    public long getPerfilEstudante()     { return perfilEstudante; }
    public long getPerfilSenior()        { return perfilSenior; }
    public long getPerfilNormal()        { return perfilNormal; }
    public OffsetDateTime getActualizadoEm() { return actualizadoEm; }
    void setTotalValidacoes(long v) { this.totalValidacoes = v; }
    void setTotalInvalidas(long v)  { this.totalInvalidas = v; }
    void setPerfilEstudante(long v) { this.perfilEstudante = v; }
    void setPerfilSenior(long v)    { this.perfilSenior = v; }
    void setPerfilNormal(long v)    { this.perfilNormal = v; }
    void setActualizadoEm(OffsetDateTime t) { this.actualizadoEm = t; }
}