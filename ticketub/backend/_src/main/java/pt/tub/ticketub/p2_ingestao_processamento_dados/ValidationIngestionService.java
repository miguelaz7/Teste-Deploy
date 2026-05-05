package pt.tub.ticketub.p2_ingestao_processamento_dados;

import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantine;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.ValidationQuarantineRepository;
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
public class ValidationIngestionService {

    private static final java.time.Duration JANELA_DUPLICADOS = java.time.Duration.ofHours(24);

    private final ControladorValidacaoPicagens validacao;
    private final ControladorNormalizacaoAnonimizacao normalizacao;
    private final ControladorPersistenciaDataLake persistencia;
    private final ControladorRegistoEstatisticas estatisticas;
    private final ValidationEventRepository validationEventRepository;
    private final ValidationQuarantineRepository validationQuarantineRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ValidationIngestionService(
        ControladorValidacaoPicagens validacao,
        ControladorNormalizacaoAnonimizacao normalizacao,
        ControladorPersistenciaDataLake persistencia,
        ControladorRegistoEstatisticas estatisticas,
        ValidationEventRepository validationEventRepository,
        ValidationQuarantineRepository validationQuarantineRepository
    ) {
        this.validacao = validacao;
        this.normalizacao = normalizacao;
        this.persistencia = persistencia;
        this.estatisticas = estatisticas;
        this.validationEventRepository = validationEventRepository;
        this.validationQuarantineRepository = validationQuarantineRepository;
    }

    @Transactional
    public ValidationIngestionResponseDto ingerir(List<ValidationIngestionRequestDto> validacoes) {
        if (validacoes == null || validacoes.isEmpty()) {
            return new ValidationIngestionResponseDto(0, 0, 0, 0);
        }

        OffsetDateTime inicioCiclo = OffsetDateTime.now();
        String batchId = UUID.randomUUID().toString();

        List<ValidationEvent> eventos = new ArrayList<>();
        List<ValidationQuarantine> quarentena = new ArrayList<>();
        List<String> motivosRejeicao = new ArrayList<>();
        Set<String> hashesPacote = new HashSet<>();
        int duplicados = 0;

        for (ValidationIngestionRequestDto dto : validacoes) {

            // Payload nulo
            if (dto == null) {
                quarentena.add(new ValidationQuarantine(
                    "{}", "Payload nulo", "payload",
                    null, "Objeto de validacao obrigatorio", OffsetDateTime.now()));
                motivosRejeicao.add("payload_nulo");
                continue;
            }

            // O0.2.1.c — Validar com regras lidas da BD (O0.2.1.d)
            ControladorValidacaoPicagens.ResultadoValidacao resultado = validacao.validar(dto);
            if (!resultado.isValido()) {
                quarentena.add(new ValidationQuarantine(
                    toJson(dto), resultado.getMotivo(), resultado.getCampo(),
                    resultado.getValorRecebido(), resultado.getRegra(), OffsetDateTime.now()));
                motivosRejeicao.add(resultado.getRuleCode());
                continue;
            }

            // O0.2.2.c — Calcular hash para deduplicação
            String hash = normalizacao.calcularIngestionHash(dto);

            if (hashesPacote.contains(hash)) {
                duplicados++;
                continue;
            }
            hashesPacote.add(hash);

            OffsetDateTime janela = OffsetDateTime.now().minus(JANELA_DUPLICADOS);
            if (validationEventRepository.existsByIngestionHashAndIngestedAtAfter(hash, janela)) {
                duplicados++;
                continue;
            }

            // O0.2.2.c — Normalizar e anonimizar
            try {
                eventos.add(normalizacao.normalizar(dto, hash));
            } catch (Exception e) {
                quarentena.add(new ValidationQuarantine(
                    toJson(dto), "Falha na normalizacao", "payload",
                    null, e.getMessage(), OffsetDateTime.now()));
                motivosRejeicao.add("falha_normalizacao");
            }
        }

        // Gravar eventos válidos (O0.2.2.d) e quarentena
        validationEventRepository.saveAll(eventos);
        validationQuarantineRepository.saveAll(quarentena);

        // O0.2.3.c — Persistir no Data Lake (O0.2.4.d)
        persistencia.persistir(eventos, batchId, inicioCiclo);

        // O0.2.4.c — Registar estatísticas do ciclo (O0.2.4.d)
        estatisticas.registar(batchId, validacoes.size(), eventos.size(),
            quarentena.size(), duplicados, motivosRejeicao, inicioCiclo);

        return new ValidationIngestionResponseDto(
            validacoes.size(), eventos.size(), quarentena.size(), duplicados);
    }

    private String toJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
