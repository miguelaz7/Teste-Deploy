package com.example.demo.dto;

public class ValidationIngestionResponseDto {

    private final int totalRecebidas;
    private final int validosPersistidos;
    private final int emQuarentena;
    private final int duplicadosDescartados;

    public ValidationIngestionResponseDto(int totalRecebidas, int validosPersistidos, int emQuarentena, int duplicadosDescartados) {
        this.totalRecebidas = totalRecebidas;
        this.validosPersistidos = validosPersistidos;
        this.emQuarentena = emQuarentena;
        this.duplicadosDescartados = duplicadosDescartados;
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
}