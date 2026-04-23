package pt.tub.ticketub.p2_ingestao_processamento_dados;

import java.util.ArrayList;
import java.util.List;

public class ValidationIngestionBatchRequestDto {

    private List<ValidationIngestionRequestDto> validacoes = new ArrayList<>();

    public List<ValidationIngestionRequestDto> getValidacoes() {
        return validacoes;
    }

    public void setValidacoes(List<ValidationIngestionRequestDto> validacoes) {
        this.validacoes = validacoes;
    }
}




