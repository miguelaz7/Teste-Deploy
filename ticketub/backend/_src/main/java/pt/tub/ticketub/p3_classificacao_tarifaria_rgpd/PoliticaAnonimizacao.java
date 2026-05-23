package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

// =============================================================================
// O0.3.2.d – Repositório de Políticas de Anonimização
// Regras RGPD aprovadas pelo DPO: campos a pseudonimizar, períodos de
// retenção e histórico de alterações. Consultado pelo UC02.2.
// =============================================================================

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Table(name = "politicas_anonimizacao")
public class PoliticaAnonimizacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Campo a pseudonimizar (ex: "cardId", "ticketId")
    @Column(name = "campo", nullable = false)
    private String campo;

    // Método de anonimização (ex: "HMAC_SHA256", "SUPRESSAO")
    @Column(name = "metodo", nullable = false)
    private String metodo;

    // Período de retenção em dias (-1 = indefinido)
    @Column(name = "retencao_dias", nullable = false)
    private int retencaoDias;

    // Estado da política: ATIVA, REVOGADA
    @Column(name = "estado", nullable = false)
    private String estado;

    // DPO que aprovou
    @Column(name = "aprovado_por", nullable = false)
    private String aprovadoPor;

    @Column(name = "aprovado_em", nullable = false)
    private OffsetDateTime aprovadoEm;

    @Column(name = "notas", length = 1000)
    private String notas;

    PoliticaAnonimizacao() {}

    PoliticaAnonimizacao(String campo, String metodo, int retencaoDias,
                         String estado, String aprovadoPor,
                         OffsetDateTime aprovadoEm, String notas) {
        this.campo       = campo;
        this.metodo      = metodo;
        this.retencaoDias = retencaoDias;
        this.estado      = estado;
        this.aprovadoPor = aprovadoPor;
        this.aprovadoEm  = aprovadoEm;
        this.notas       = notas;
    }

    public Long getId()                    { return id; }
    public String getCampo()               { return campo; }
    public void setCampo(String campo)     { this.campo = campo; }
    public String getMetodo()              { return metodo; }
    public void setMetodo(String metodo)   { this.metodo = metodo; }
    public int getRetencaoDias()           { return retencaoDias; }
    public void setRetencaoDias(int d)     { this.retencaoDias = d; }
    public String getEstado()              { return estado; }
    public void setEstado(String estado)   { this.estado = estado; }
    public String getAprovadoPor()         { return aprovadoPor; }
    public void setAprovadoPor(String a)   { this.aprovadoPor = a; }
    public OffsetDateTime getAprovadoEm()  { return aprovadoEm; }
    public void setAprovadoEm(OffsetDateTime a) { this.aprovadoEm = a; }
    public String getNotas()               { return notas; }
    public void setNotas(String notas)     { this.notas = notas; }
}

@Repository
interface PoliticaAnonimizacaoRepository extends JpaRepository<PoliticaAnonimizacao, Long> {
    List<PoliticaAnonimizacao> findByEstado(String estado);
    List<PoliticaAnonimizacao> findByCampo(String campo);
}