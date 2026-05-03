package pt.tub.ticketub.p4_interfaces_utilizador;

public class ValidationDashboardMetricsDto {

    private PeriodMetrics ultimaHora;
    private PeriodMetrics ultimas24Horas;
    private PeriodMetrics ultimos7Dias;

    public ValidationDashboardMetricsDto(PeriodMetrics ultimaHora, PeriodMetrics ultimas24Horas, PeriodMetrics ultimos7Dias) {
        this.ultimaHora = ultimaHora;
        this.ultimas24Horas = ultimas24Horas;
        this.ultimos7Dias = ultimos7Dias;
    }

    public PeriodMetrics getUltimaHora() {
        return ultimaHora;
    }

    public PeriodMetrics getUltimas24Horas() {
        return ultimas24Horas;
    }

    public PeriodMetrics getUltimos7Dias() {
        return ultimos7Dias;
    }

    public static class PeriodMetrics {
        private String periodo;
        private long volumeIngestao;
        private long validas;
        private long quarentena;
        private long duplicados;
        private double taxaValidas;
        private double taxaQuarentena;

        public PeriodMetrics(
            String periodo,
            long volumeIngestao,
            long validas,
            long quarentena,
            long duplicados,
            double taxaValidas,
            double taxaQuarentena
        ) {
            this.periodo = periodo;
            this.volumeIngestao = volumeIngestao;
            this.validas = validas;
            this.quarentena = quarentena;
            this.duplicados = duplicados;
            this.taxaValidas = taxaValidas;
            this.taxaQuarentena = taxaQuarentena;
        }

        public String getPeriodo() {
            return periodo;
        }

        public long getVolumeIngestao() {
            return volumeIngestao;
        }

        public long getValidas() {
            return validas;
        }

        public long getQuarentena() {
            return quarentena;
        }

        public long getDuplicados() {
            return duplicados;
        }

        public double getTaxaValidas() {
            return taxaValidas;
        }

        public double getTaxaQuarentena() {
            return taxaQuarentena;
        }
    }
}





