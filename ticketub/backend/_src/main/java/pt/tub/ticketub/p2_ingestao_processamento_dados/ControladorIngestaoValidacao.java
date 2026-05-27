package pt.tub.ticketub.p2_ingestao_processamento_dados;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/validations")
public class ControladorIngestaoValidacao {

    private final ServicoIngestaoValidacao servicoIngestaoValidacao;

    public ControladorIngestaoValidacao(ServicoIngestaoValidacao servicoIngestaoValidacao) {
        this.servicoIngestaoValidacao = servicoIngestaoValidacao;
    }

    @PostMapping("/ingest")
    public ResponseEntity<DtoRespostaIngestaoValidacao> ingestValidation(@RequestBody DtoPedidoIngestaoValidacao validacao) {
        DtoRespostaIngestaoValidacao resultado = servicoIngestaoValidacao.ingest(List.of(validacao));
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/ingest/batch")
    public ResponseEntity<DtoRespostaIngestaoValidacao> ingestBatch(@RequestBody DtoPedidoLoteIngestaoValidacao request) {
        DtoRespostaIngestaoValidacao resultado = servicoIngestaoValidacao.ingest(request.getValidacoes());
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }
}
