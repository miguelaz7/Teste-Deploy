package pt.tub.ticketub.p2_ingestao;



import pt.tub.ticketub.p9_interoperabilidade.NgsiLdEntityDto;

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







