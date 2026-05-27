package pt.tub.ticketub.p2_ingestao_processamento_dados;

import java.util.ArrayList;
import java.util.List;

public class DtoPedidoLoteIngestaoValidacao {

    private List<DtoPedidoIngestaoValidacao> validacoes = new ArrayList<>();

    public List<DtoPedidoIngestaoValidacao> getValidacoes() {
        return validacoes;
    }

    public void setValidacoes(List<DtoPedidoIngestaoValidacao> validacoes) {
        this.validacoes = validacoes;
    }
}
