package pt.tub.ticketub.p2_ingestao_processamento_dados;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

// =============================================================================
// O0.2.4.d – Repositório de Estatísticas de Ingestão
// Armazena contadores de ingestão, tempos de processamento,
// número de válidos, inválidos e duplicados, e resumo de auditoria do lote.
// =============================================================================

@Entity
@Table(name = "ingestion_batch_stats")
class IngestionBatchStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "timestamp_ciclo", nullable = false)
    private OffsetDateTime timestampCiclo;

    @Column(name = "total_recebidos", nullable = false)
    private int totalRecebidos;

    @Column(name = "total_validos", nullable = false)
    private int totalValidos;

    @Column(name = "total_invalidos", nullable = false)
    private int totalInvalidos;

    @Column(name = "total_duplicados", nullable = false)
    private int totalDuplicados;

    @Column(name = "tempo_processamento_ms", nullable = false)
    private long tempoProcessamentoMs;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "motivos_rejeicao", length = 2000)
    private String motivosRejeicao;

    @Column(name = "alerta_qualidade", nullable = false)
    private boolean alertaQualidade;

    IngestionBatchStats() {}

    IngestionBatchStats(String batchId, OffsetDateTime timestampCiclo,
                        int totalRecebidos, int totalValidos,
                        int totalInvalidos, int totalDuplicados,
                        long tempoProcessamentoMs, String estado,
                        String motivosRejeicao, boolean alertaQualidade) {
        this.batchId = batchId;
        this.timestampCiclo = timestampCiclo;
        this.totalRecebidos = totalRecebidos;
        this.totalValidos = totalValidos;
        this.totalInvalidos = totalInvalidos;
        this.totalDuplicados = totalDuplicados;
        this.tempoProcessamentoMs = tempoProcessamentoMs;
        this.estado = estado;
        this.motivosRejeicao = motivosRejeicao;
        this.alertaQualidade = alertaQualidade;
    }

    String getBatchId() { return batchId; }
    OffsetDateTime getTimestampCiclo() { return timestampCiclo; }
    int getTotalRecebidos() { return totalRecebidos; }
    int getTotalValidos() { return totalValidos; }
    int getTotalInvalidos() { return totalInvalidos; }
    int getTotalDuplicados() { return totalDuplicados; }
    long getTempoProcessamentoMs() { return tempoProcessamentoMs; }
    String getEstado() { return estado; }
    String getMotivosRejeicao() { return motivosRejeicao; }
    boolean isAlertaQualidade() { return alertaQualidade; }
}

@Repository
interface IngestionBatchStatsRepository extends JpaRepository<IngestionBatchStats, Long> {
}
