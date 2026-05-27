package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

// =============================================================================
// O0.3.3.d – Repositório de Eventos Não Categorizados
// Eventos com tipologia desconhecida: motivo de rejeição e estado
// (PENDENTE / RECLASSIFICADO / REJEITADO).
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
@Table(name = "eventos_nao_categorizados")
public class EventoNaoCategorizado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Referência ao evento original via ingestion hash
    @Column(name = "ingestion_hash", nullable = false)
    private String ingestionHash;

    // Motivo pelo qual não foi possível categorizar
    @Column(name = "motivo_rejeicao", nullable = false, length = 500)
    private String motivoRejeicao;

    // Estado: PENDENTE, RECLASSIFICADO, REJEITADO
    @Column(name = "estado", nullable = false)
    private String status;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolvido_por")
    private String resolvedBy;

    EventoNaoCategorizado() {}

    EventoNaoCategorizado(String ingestionHash, String motivoRejeicao,
                          String status, OffsetDateTime createdAt) {
        this.ingestionHash  = ingestionHash;
        this.motivoRejeicao = motivoRejeicao;
        this.status         = status;
        this.createdAt       = createdAt;
    }

    public Long getId()                           { return id; }
    public String getIngestionHash()              { return ingestionHash; }
    public String getMotivoRejeicao()             { return motivoRejeicao; }
    public String getStatus()                     { return status; }
    public void setStatus(String status)          { this.status = status; }
    public OffsetDateTime getCreatedAt()          { return createdAt; }
    public OffsetDateTime getResolvedAt()        { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime t)  { this.resolvedAt = t; }
    public String getResolvedBy()               { return resolvedBy; }
    public void setResolvedBy(String r)         { this.resolvedBy = r; }
}

@Repository
interface RepositorioEventoNaoCategorizado extends JpaRepository<EventoNaoCategorizado, Long> {
    List<EventoNaoCategorizado> findByStatus(String status);
    long countByStatus(String status);
}