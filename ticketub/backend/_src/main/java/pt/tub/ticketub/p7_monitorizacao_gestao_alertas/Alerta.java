package pt.tub.ticketub.p7_monitorizacao_gestao_alertas;

// =============================================================================
// O0.9.1.d – Repositório de Alertas e Anomalias
// Repositório de alertas: tipo, severidade, timestamp, dados do evento
// e estado (PENDENTE / EM_ANALISE / RESOLVIDO / FALSO_POSITIVO).
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
@Table(name = "alertas_anomalias")
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "severidade", nullable = false)
    private String severidade;

    @Column(name = "ingestion_hash")
    private String ingestionHash;

    @Column(name = "dados_evento", length = 2000)
    private String dadosEvento;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "atribuido_a")
    private String atribuidoA;

    @Column(name = "accao_resolucao", length = 1000)
    private String accaoResolucao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvidoEm;

    public Alerta() {}

    public Alerta(String tipo, String severidade, String ingestionHash,
                  String dadosEvento, OffsetDateTime criadoEm) {
        this.tipo          = tipo;
        this.severidade    = severidade;
        this.ingestionHash = ingestionHash;
        this.dadosEvento   = dadosEvento;
        this.estado        = "PENDENTE";
        this.criadoEm      = criadoEm;
    }

    public Long getId()                          { return id; }
    public String getTipo()                      { return tipo; }
    public String getSeveridade()                { return severidade; }
    public String getIngestionHash()             { return ingestionHash; }
    public String getDadosEvento()               { return dadosEvento; }
    public String getEstado()                    { return estado; }
    public void setEstado(String estado)         { this.estado = estado; }
    public String getAtribuidoA()                { return atribuidoA; }
    public void setAtribuidoA(String a)          { this.atribuidoA = a; }
    public String getAccaoResolucao()            { return accaoResolucao; }
    public void setAccaoResolucao(String a)      { this.accaoResolucao = a; }
    public OffsetDateTime getCriadoEm()          { return criadoEm; }
    public OffsetDateTime getResolvidoEm()       { return resolvidoEm; }
    public void setResolvidoEm(OffsetDateTime t) { this.resolvidoEm = t; }
}
