package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

public class CategorizationStatsDto {

    private long totalClassificados;
    private long totalNaoCategorizado;
    private long estudante;
    private long senior;
    private long normal;

    public CategorizationStatsDto(long totalClassificados, long totalNaoCategorizado,
                                   long estudante, long senior, long normal) {
        this.totalClassificados = totalClassificados;
        this.totalNaoCategorizado = totalNaoCategorizado;
        this.estudante = estudante;
        this.senior = senior;
        this.normal = normal;
    }

    public long getTotalClassificados() { return totalClassificados; }
    public long getTotalNaoCategorizado() { return totalNaoCategorizado; }
    public long getEstudante() { return estudante; }
    public long getSenior() { return senior; }
    public long getNormal() { return normal; }
}





