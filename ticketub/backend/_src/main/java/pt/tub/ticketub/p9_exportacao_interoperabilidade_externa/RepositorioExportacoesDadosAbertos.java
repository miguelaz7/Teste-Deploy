package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

// =============================================================================
// O012.1.d – Repositório de Exportações de Dados Abertos
// Ficheiros de exportação com metadados: utilizador, data, formato,
// filtros aplicados e estado de aprovação DPO.
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
@Table(name = "exportacoes_dados_abertos")
class ExportacaoDadosAbertos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Utilizador que solicitou a exportação
    @Column(name = "solicitado_por", nullable = false)
    private String solicitadoPor;

    // Formato: JSON, CSV
    @Column(name = "formato", nullable = false)
    private String formato;

    // Filtros aplicados (ex: "route_id=12;periodo=2026-01")
    @Column(name = "filtros", length = 1000)
    private String filtros;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodoFim;

    @Column(name = "total_registos", nullable = false)
    private long totalRegistos;

    // Estado: PENDENTE_DPO, APROVADA, REJEITADA, EXPORTADA
    @Column(name = "estado", nullable = false)
    private String estado;

    // DPO que aprovou/rejeitou
    @Column(name = "aprovado_por")
    private String aprovadoPor;

    @Column(name = "aprovado_em")
    private OffsetDateTime aprovadoEm;

    // Hash de integridade do ficheiro exportado
    @Column(name = "hash_ficheiro")
    private String hashFicheiro;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    ExportacaoDadosAbertos() {}

    ExportacaoDadosAbertos(String solicitadoPor, String formato, String filtros,
                           LocalDate periodoInicio, LocalDate periodoFim,
                           long totalRegistos, OffsetDateTime criadoEm) {
        this.solicitadoPor = solicitadoPor;
        this.formato       = formato;
        this.filtros       = filtros;
        this.periodoInicio = periodoInicio;
        this.periodoFim    = periodoFim;
        this.totalRegistos = totalRegistos;
        this.estado        = "PENDENTE_DPO";
        this.criadoEm      = criadoEm;
    }

    Long getId()                          { return id; }
    String getSolicitadoPor()             { return solicitadoPor; }
    String getFormato()                   { return formato; }
    String getFiltros()                   { return filtros; }
    LocalDate getPeriodoInicio()          { return periodoInicio; }
    LocalDate getPeriodoFim()             { return periodoFim; }
    long getTotalRegistos()               { return totalRegistos; }
    String getEstado()                    { return estado; }
    void setEstado(String estado)         { this.estado = estado; }
    String getAprovadoPor()               { return aprovadoPor; }
    void setAprovadoPor(String a)         { this.aprovadoPor = a; }
    OffsetDateTime getAprovadoEm()        { return aprovadoEm; }
    void setAprovadoEm(OffsetDateTime t)  { this.aprovadoEm = t; }
    String getHashFicheiro()              { return hashFicheiro; }
    void setHashFicheiro(String h)        { this.hashFicheiro = h; }
    OffsetDateTime getCriadoEm()          { return criadoEm; }
}

@Repository
interface ExportacaoDadosAbertosRepository extends JpaRepository<ExportacaoDadosAbertos, Long> {
    List<ExportacaoDadosAbertos> findByEstado(String estado);
    List<ExportacaoDadosAbertos> findBySolicitadoPor(String solicitadoPor);
}
