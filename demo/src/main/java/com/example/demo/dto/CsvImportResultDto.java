package com.example.demo.dto;

public class CsvImportResultDto {

    private int totalRecebidas;
    private int validosPersistidos;
    private int emQuarentena;
    private int duplicadosDescartados;
    private long tempoValidacaoMs;
    private long tempoNormalizacaoMs;
    private long tempoPersistenciaMs;
    private String etlMappingVersion;
    private String batchId;
    private boolean persistenciaConfirmada;
    private String estadoLote;
    private java.util.List<NgsiLdEntityDto> entidadesNgsiLd;

    public CsvImportResultDto(
        int totalRecebidas,
        int validosPersistidos,
        int emQuarentena,
        int duplicadosDescartados,
        long tempoValidacaoMs,
        long tempoNormalizacaoMs,
        long tempoPersistenciaMs,
        String etlMappingVersion,
        String batchId,
        boolean persistenciaConfirmada,
        String estadoLote,
        java.util.List<NgsiLdEntityDto> entidadesNgsiLd
    ) {
        this.totalRecebidas = totalRecebidas;
        this.validosPersistidos = validosPersistidos;
        this.emQuarentena = emQuarentena;
        this.duplicadosDescartados = duplicadosDescartados;
        this.tempoValidacaoMs = tempoValidacaoMs;
        this.tempoNormalizacaoMs = tempoNormalizacaoMs;
        this.tempoPersistenciaMs = tempoPersistenciaMs;
        this.etlMappingVersion = etlMappingVersion;
        this.batchId = batchId;
        this.persistenciaConfirmada = persistenciaConfirmada;
        this.estadoLote = estadoLote;
        this.entidadesNgsiLd = entidadesNgsiLd;
    }

    public int getTotalRecebidas() {
        return totalRecebidas;
    }

    public int getValidosPersistidos() {
        return validosPersistidos;
    }

    public int getEmQuarentena() {
        return emQuarentena;
    }

    public int getDuplicadosDescartados() {
        return duplicadosDescartados;
    }

    public long getTempoValidacaoMs() {
        return tempoValidacaoMs;
    }

    public long getTempoNormalizacaoMs() {
        return tempoNormalizacaoMs;
    }

    public long getTempoPersistenciaMs() {
        return tempoPersistenciaMs;
    }

    public String getEtlMappingVersion() {
        return etlMappingVersion;
    }

    public String getBatchId() {
        return batchId;
    }

    public boolean isPersistenciaConfirmada() {
        return persistenciaConfirmada;
    }

    public String getEstadoLote() {
        return estadoLote;
    }

    public java.util.List<NgsiLdEntityDto> getEntidadesNgsiLd() {
        return entidadesNgsiLd;
    }

    // Compatibilidade temporaria com nomes anteriores.
    public int getTotalLinhas() {
        return totalRecebidas;
    }

    public int getLinhasImportadas() {
        return validosPersistidos;
    }

    public int getLinhasIgnoradas() {
        return emQuarentena;
    }
}