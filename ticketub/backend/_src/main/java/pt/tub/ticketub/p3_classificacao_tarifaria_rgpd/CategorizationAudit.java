package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "categorization_audit")
public class CategorizationAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "acao", nullable = false)
    private String acao;

    @Column(name = "tipo_titulo", nullable = false)
    private String tipoTitulo;

    @Column(name = "perfil_anterior")
    private String perfilAnterior;

    @Column(name = "perfil_novo")
    private String perfilNovo;

    @Column(name = "timestamp")
    private OffsetDateTime timestamp;

    @Column(name = "utilizador")
    private String utilizador;

    public CategorizationAudit() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAcao() { return acao; }
    public void setAcao(String acao) { this.acao = acao; }

    public String getTipoTitulo() { return tipoTitulo; }
    public void setTipoTitulo(String tipoTitulo) { this.tipoTitulo = tipoTitulo; }

    public String getPerfilAnterior() { return perfilAnterior; }
    public void setPerfilAnterior(String perfilAnterior) { this.perfilAnterior = perfilAnterior; }

    public String getPerfilNovo() { return perfilNovo; }
    public void setPerfilNovo(String perfilNovo) { this.perfilNovo = perfilNovo; }

    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }

    public String getUtilizador() { return utilizador; }
    public void setUtilizador(String utilizador) { this.utilizador = utilizador; }

}





