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
class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Tipo de anomalia: TITULO_EXPIRADO, DUPLICADO, TITULO_INVALIDO, DESVIO_VALIDACOES
    @Column(name = "tipo", nullable = false)
    private String tipo;

    // Severidade: CRITICO, AVISO, INFO
    @Column(name = "severidade", nullable = false)
    private String severidade;

    // Hash do evento que gerou o alerta
    @Column(name = "ingestion_hash")
    private String ingestionHash;

    // Dados resumidos do evento para contexto
    @Column(name = "dados_evento", length = 2000)
    private String dadosEvento;

    // Estado: PENDENTE, EM_ANALISE, RESOLVIDO, FALSO_POSITIVO
    @Column(name = "estado", nullable = false)
    private String estado;

    // Técnico ou equipa a quem foi atribuído
    @Column(name = "atribuido_a")
    private String atribuidoA;

    // Acção de resolução registada
    @Column(name = "accao_resolucao", length = 1000)
    private String accaoResolucao;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvidoEm;

    Alerta() {}

    Alerta(String tipo, String severidade, String ingestionHash,
           String dadosEvento, OffsetDateTime criadoEm) {
        this.tipo          = tipo;
        this.severidade    = severidade;
        this.ingestionHash = ingestionHash;
        this.dadosEvento   = dadosEvento;
        this.estado        = "PENDENTE";
        this.criadoEm      = criadoEm;
    }

    Long getId()                          { return id; }
    String getTipo()                      { return tipo; }
    String getSeveridade()                { return severidade; }
    String getIngestionHash()             { return ingestionHash; }
    String getDadosEvento()               { return dadosEvento; }
    String getEstado()                    { return estado; }
    void setEstado(String estado)         { this.estado = estado; }
    String getAtribuidoA()                { return atribuidoA; }
    void setAtribuidoA(String a)          { this.atribuidoA = a; }
    String getAccaoResolucao()            { return accaoResolucao; }
    void setAccaoResolucao(String a)      { this.accaoResolucao = a; }
    OffsetDateTime getCriadoEm()          { return criadoEm; }
    OffsetDateTime getResolvidoEm()       { return resolvidoEm; }
    void setResolvidoEm(OffsetDateTime t) { this.resolvidoEm = t; }
}

@Repository
interface AlertaRepository extends JpaRepository<Alerta, Long> {
    List<Alerta> findByEstado(String estado);
    List<Alerta> findByTipoAndEstado(String tipo, String estado);
    long countByEstadoAndCriadoEmAfter(String estado, OffsetDateTime reference);
    long countByTipo(String tipo);
}
