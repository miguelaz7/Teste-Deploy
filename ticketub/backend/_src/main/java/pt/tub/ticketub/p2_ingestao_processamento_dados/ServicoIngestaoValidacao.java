package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p3_classificacao_tarifaria_rgpd.ControladorClassificacaoTarifaria;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.QuarentenaValidacao;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioQuarentenaValidacao;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// =============================================================================
// Orquestrador do UC02 — liga os 4 controladores pela ordem correcta:
//   1. ControladorValidacaoPicagens     (O0.2.1.c)
//   2. ControladorNormalizacaoAnonimizacao (O0.2.2.c)
//   3. ControladorPersistenciaDataLake  (O0.2.3.c)
//   4. ControladorRegistoEstatisticas   (O0.2.4.c)
// =============================================================================

@Service
public class ServicoIngestaoValidacao {

    private static final java.time.Duration JANELA_DUPLICADOS = java.time.Duration.ofHours(24);

    private final ControladorClassificacaoTarifaria classificacao;
    private final ControladorValidacaoPicagens validacao;
    private final ControladorNormalizacaoAnonimizacao normalizacao;
    private final ControladorPersistenciaDataLake persistencia;
    private final ControladorRegistoEstatisticas estatisticas;
    private final RepositorioEventoValidacao repositorioEventoValidacao;
    private final RepositorioQuarentenaValidacao repositorioQuarentenaValidacao;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServicoIngestaoValidacao(
        ControladorClassificacaoTarifaria classificacao,
        ControladorValidacaoPicagens validacao,
        ControladorNormalizacaoAnonimizacao normalizacao,
        ControladorPersistenciaDataLake persistencia,
        ControladorRegistoEstatisticas estatisticas,
        RepositorioEventoValidacao repositorioEventoValidacao,
        RepositorioQuarentenaValidacao repositorioQuarentenaValidacao
    ) {
        this.classificacao = classificacao;
        this.validacao = validacao;
        this.normalizacao = normalizacao;
        this.persistencia = persistencia;
        this.estatisticas = estatisticas;
        this.repositorioEventoValidacao = repositorioEventoValidacao;
        this.repositorioQuarentenaValidacao = repositorioQuarentenaValidacao;
    }

    @Transactional
    public DtoRespostaIngestaoValidacao ingest(List<DtoPedidoIngestaoValidacao> validacoes) {
        if (validacoes == null || validacoes.isEmpty()) {
            return new DtoRespostaIngestaoValidacao(0, 0, 0, 0);
        }

        OffsetDateTime inicioCiclo = OffsetDateTime.now();
        String batchId = UUID.randomUUID().toString();

        List<EventoValidacao> eventos = new ArrayList<>();
        List<QuarentenaValidacao> quarentena = new ArrayList<>();
        List<String> motivosRejeicao = new ArrayList<>();
        Set<String> hashesPacote = new HashSet<>();
        int duplicados = 0;

        for (DtoPedidoIngestaoValidacao dto : validacoes) {

            if (dto == null) {
                quarentena.add(new QuarentenaValidacao(
                    "{}", "Payload nulo", "payload",
                    null, "Objeto de validacao obrigatorio",
                    null, OffsetDateTime.now()));
                motivosRejeicao.add("payload_nulo");
                continue;
            }

            // O0.2.1.c — Validar com regras lidas da BD (O0.2.1.d)
            ControladorValidacaoPicagens.ValidationResult resultado = validacao.validate(dto);
            if (!resultado.isValid()) {
                // Passa o originStopId para a quarentena (UC06.3)
                quarentena.add(new QuarentenaValidacao(
                    toJson(dto), resultado.getMotivo(), resultado.getCampo(),
                    resultado.getValorRecebido(), resultado.getRegra(),
                    dto.getOriginStopId(), OffsetDateTime.now()));
                motivosRejeicao.add(resultado.getRuleCode());
                continue;
            }

            // O0.2.2.c — Calcular hash para deduplicação
            String hash = normalizacao.calculateIngestionHash(dto);

            if (hashesPacote.contains(hash)) {
                duplicados++;
                continue;
            }
            hashesPacote.add(hash);

            OffsetDateTime JANELA = OffsetDateTime.now().minus(JANELA_DUPLICADOS);
            if (repositorioEventoValidacao.existsByIngestionHashAndIngestedAtAfter(hash, JANELA)) {
                duplicados++;
                continue;
            }

            // O0.2.2.c — Normalizar e anonimizar
            try {
                eventos.add(normalizacao.normalize(dto, hash));
            } catch (Exception e) {
                quarentena.add(new QuarentenaValidacao(
                    toJson(dto), "Falha na normalizacao", "payload",
                    null, e.getMessage(),
                    dto.getOriginStopId(), OffsetDateTime.now()));
                motivosRejeicao.add("falha_normalizacao");
            }
        }

        // Gravar eventos válidos (O0.2.2.d) e quarentena
        repositorioEventoValidacao.saveAll(eventos);
        repositorioQuarentenaValidacao.saveAll(quarentena);

        // O0.2.3.c — Persistir no Data Lake (O0.2.4.d)
        persistencia.persist(eventos, batchId, inicioCiclo);
        if (!eventos.isEmpty()) classificacao.classify();

        // O0.2.4.c — Registar estatísticas do ciclo (O0.2.4.d)
        estatisticas.register(batchId, validacoes.size(), eventos.size(),
            quarentena.size(), duplicados, motivosRejeicao, inicioCiclo);

        return new DtoRespostaIngestaoValidacao(
            validacoes.size(), eventos.size(), quarentena.size(), duplicados);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
