package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exportacoes_dados_abertos")
public class ExportacaoDadosAbertos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Utilizador que solicitou a exportação
    @Column(name = "solicitado_por", nullable = false)
    private String requestedBy;

    // Formato: JSON, CSV
    @Column(name = "formato", nullable = false)
    private String format;

    // Filtros aplicados (ex: "route_id=12;periodo=2026-01")
    @Column(name = "filtros", length = 1000)
    private String filters;

    @Column(name = "periodo_inicio", nullable = false)
    private LocalDate periodStart;

    @Column(name = "periodo_fim", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "total_registos", nullable = false)
    private long totalRecords;

    // Estado: PENDENTE_DPO, APROVADA, REJEITADA, EXPORTADA
    @Column(name = "estado", nullable = false)
    private String status;

    // DPO que aprovou/rejeitou
    @Column(name = "aprovado_por")
    private String approvedBy;

    @Column(name = "aprovado_em")
    private OffsetDateTime approvedAt;

    // Hash de integridade do ficheiro exportado
    @Column(name = "hash_ficheiro")
    private String fileHash;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime createdAt;

    public ExportacaoDadosAbertos() {}

    public ExportacaoDadosAbertos(String requestedBy, String format, String filters,
                          LocalDate periodStart, LocalDate periodEnd,
                          long totalRecords, OffsetDateTime createdAt) {
        this.requestedBy = requestedBy;
        this.format      = format;
        this.filters      = filters;
        this.periodStart = periodStart;
        this.periodEnd   = periodEnd;
        this.totalRecords = totalRecords;
        this.status      = "PENDENTE_DPO";
        this.createdAt   = createdAt;
    }

    public Long getId()                          { return id; }
    public String getRequestedBy()               { return requestedBy; }
    public String getFormat()                    { return format; }
    public String getFilters()                   { return filters; }
    public LocalDate getPeriodStart()            { return periodStart; }
    public LocalDate getPeriodEnd()              { return periodEnd; }
    public long getTotalRecords()                { return totalRecords; }
    public String getStatus()                    { return status; }
    public void setStatus(String status)         { this.status = status; }
    public String getApprovedBy()                { return approvedBy; }
    public void setApprovedBy(String a)          { this.approvedBy = a; }
    public OffsetDateTime getApprovedAt()        { return approvedAt; }
    public void setApprovedAt(OffsetDateTime t)  { this.approvedAt = t; }
    public String getFileHash()                  { return fileHash; }
    public void setFileHash(String h)            { this.fileHash = h; }
    public OffsetDateTime getCreatedAt()         { return createdAt; }
}
