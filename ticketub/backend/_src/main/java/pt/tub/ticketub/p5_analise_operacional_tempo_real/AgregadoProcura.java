package pt.tub.ticketub.p5_analise_operacional_tempo_real;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "agregados_procura")
public class AgregadoProcura {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "perspectiva", nullable = false)
    private String perspectiva;
    
    @Column(name = "chave", nullable = false)
    private String chave;

    @Column(name = "descricao")
    private String descricao;

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

    public AgregadoProcura() {}
    public AgregadoProcura(String perspectiva, String chave, String descricao, long totalValidacoes, long totalInvalidas, long perfilEstudante, long perfilSenior, long perfilNormal, OffsetDateTime actualizadoEm) {
        this.perspectiva = perspectiva; 
        this.chave = chave; 
        this.descricao = descricao;
        this.totalValidacoes = totalValidacoes; 
        this.totalInvalidas = totalInvalidas; 
        this.perfilEstudante = perfilEstudante; 
        this.perfilSenior = perfilSenior; 
        this.perfilNormal = perfilNormal; 
        this.actualizadoEm = actualizadoEm;
    }

    public Long getId() { return id; }
    public String getPerspectiva() { return perspectiva; }
    public String getChave() { return chave; }
    public String getDescricao() { return descricao; }
    public long getTotalValidacoes() { return totalValidacoes; }
    public long getTotalInvalidas() { return totalInvalidas; }
    public long getPerfilEstudante() { return perfilEstudante; }
    public long getPerfilSenior() { return perfilSenior; }
    public long getPerfilNormal() { return perfilNormal; }
    public OffsetDateTime getActualizadoEm() { return actualizadoEm; }
    
    public void setChave(String chave) { this.chave = chave; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setTotalValidacoes(long v) { this.totalValidacoes = v; }
    public void setTotalInvalidas(long v) { this.totalInvalidas = v; }
    public void setPerfilEstudante(long v) { this.perfilEstudante = v; }
    public void setPerfilSenior(long v) { this.perfilSenior = v; }
    public void setPerfilNormal(long v) { this.perfilNormal = v; }
    public void setActualizadoEm(OffsetDateTime t) { this.actualizadoEm = t; }
}
