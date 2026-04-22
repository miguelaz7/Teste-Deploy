package com.example.demo.dto;

import java.util.List;

public record CsvImportResultDto(
    int totalLinhas,
    int eventosImportados,
    int quarentena,
    int duplicadosDescartados,
    long tempoValidacaoMs,
    long tempoNormalizacaoMs,
    long tempoPersistenciaMs,
    String etlMappingVersion,
    String batchId,
    boolean persistenciaConfirmada,
    String estadoLote,
    List<NgsiLdEntityDto> entidadesNgsiLd
) {}
