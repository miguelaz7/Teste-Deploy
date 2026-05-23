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
    private String estado;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "resolvido_em")
    private OffsetDateTime resolvidoEm;

    @Column(name = "resolvido_por")
    private String resolvidoPor;

    EventoNaoCategorizado() {}

    EventoNaoCategorizado(String ingestionHash, String motivoRejeicao,
                          String estado, OffsetDateTime criadoEm) {
        this.ingestionHash  = ingestionHash;
        this.motivoRejeicao = motivoRejeicao;
        this.estado         = estado;
        this.criadoEm       = criadoEm;
    }

    public Long getId()                           { return id; }
    public String getIngestionHash()              { return ingestionHash; }
    public String getMotivoRejeicao()             { return motivoRejeicao; }
    public String getEstado()                     { return estado; }
    public void setEstado(String estado)          { this.estado = estado; }
    public OffsetDateTime getCriadoEm()           { return criadoEm; }
    public OffsetDateTime getResolvidoEm()        { return resolvidoEm; }
    public void setResolvidoEm(OffsetDateTime t)  { this.resolvidoEm = t; }
    public String getResolvidoPor()               { return resolvidoPor; }
    public void setResolvidoPor(String r)         { this.resolvidoPor = r; }
}

@Repository
interface EventoNaoCategorizadoRepository extends JpaRepository<EventoNaoCategorizado, Long> {
    List<EventoNaoCategorizado> findByEstado(String estado);
    long countByEstado(String estado);
}