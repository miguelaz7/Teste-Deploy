package pt.tub.ticketub.p2_ingestao_processamento_dados;

// O0.2.4.d – Repositório de Estatísticas de Ingestão (entidade de estatísticas)

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ingestion_batch_stats")
public class EstatisticasLoteIngestao {

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
    private boolean qualityAlert;

    public EstatisticasLoteIngestao() {}

    public EstatisticasLoteIngestao(String batchId, OffsetDateTime timestampCiclo,
                               int totalRecebidos, int totalValidos,
                               int totalInvalidos, int totalDuplicados,
                               long tempoProcessamentoMs, String estado,
                               String motivosRejeicao, boolean qualityAlert) {
        this.batchId              = batchId;
        this.timestampCiclo       = timestampCiclo;
        this.totalRecebidos       = totalRecebidos;
        this.totalValidos         = totalValidos;
        this.totalInvalidos       = totalInvalidos;
        this.totalDuplicados      = totalDuplicados;
        this.tempoProcessamentoMs = tempoProcessamentoMs;
        this.estado               = estado;
        this.motivosRejeicao      = motivosRejeicao;
        this.qualityAlert         = qualityAlert;
    }

    public String getBatchId()               { return batchId; }
    public OffsetDateTime getTimestampCiclo() { return timestampCiclo; }
    public int getTotalRecebidos()           { return totalRecebidos; }
    public int getTotalValidos()             { return totalValidos; }
    public int getTotalInvalidos()           { return totalInvalidos; }
    public int getTotalDuplicados()          { return totalDuplicados; }
    public long getTempoProcessamentoMs()    { return tempoProcessamentoMs; }
    public String getEstado()                { return estado; }
    public String getMotivosRejeicao()       { return motivosRejeicao; }
    public boolean isQualityAlert()          { return qualityAlert; }
}
