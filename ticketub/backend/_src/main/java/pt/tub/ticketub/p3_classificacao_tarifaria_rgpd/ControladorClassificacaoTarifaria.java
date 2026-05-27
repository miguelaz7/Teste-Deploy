package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

// =============================================================================
// O0.3.1.c – Controlador de Classificação Tarifária
// Lê a tipologia de cada evento, consulta a tabela de mapeamento (O0.3.1.d)
// e atribui o perfil tarifário. Eventos sem correspondência vão para o
// Repositório de Eventos Não Categorizados (O0.3.3.d).
// =============================================================================

import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ControladorClassificacaoTarifaria {

    private static final String NAO_CATEGORIZADO = "nao_categorizado";

    private final RepositorioMapeamentoTipologiaPerfil mappingRepository;
    private final RepositorioAuditoriaCategorizacao auditRepository;
    private final RepositorioEventoValidacao validationEventRepository;
    private final RepositorioEventoNaoCategorizado eventoNaoCategorizadoRepository;

    public ControladorClassificacaoTarifaria(
        RepositorioMapeamentoTipologiaPerfil mappingRepository,
        RepositorioAuditoriaCategorizacao auditRepository,
        RepositorioEventoValidacao validationEventRepository,
        RepositorioEventoNaoCategorizado eventoNaoCategorizadoRepository
    ) {
        this.mappingRepository              = mappingRepository;
        this.auditRepository                = auditRepository;
        this.validationEventRepository      = validationEventRepository;
        this.eventoNaoCategorizadoRepository = eventoNaoCategorizadoRepository;
    }

    // Classifica todos os eventos pendentes consultando O0.3.1.d
    @Transactional
    public void classify() {
        List<EventoValidacao> pendentes = validationEventRepository
            .findByPerfilClassificadoIsNullOrPerfilClassificado(NAO_CATEGORIZADO);

        if (pendentes.isEmpty()) return;

        Map<String, String> cache = buildMappingCache();

        for (EventoValidacao event : pendentes) {
            String ticketCode = event.getTicketType() != null
                ? event.getTicketType().getCode() : null;

            if (ticketCode == null) {
                event.setPerfilClassificado(NAO_CATEGORIZADO);
                registerUncategorizedEvent(event, "Tipo de título nulo");
                continue;
            }

            String perfil = cache.get(ticketCode);
            if (perfil == null) {
                // Sem correspondência → vai para O0.3.3.d
                event.setPerfilClassificado(NAO_CATEGORIZADO);
                registerUncategorizedEvent(event, "Sem mapeamento para: " + ticketCode);
            } else {
                event.setPerfilClassificado(perfil);
            }
        }

        validationEventRepository.saveAll(pendentes);
    }

    public DtoEstatisticasCategorizacao getStats() {
        long estudante = validationEventRepository.countByPerfilClassificado("estudante");
        long senior    = validationEventRepository.countByPerfilClassificado("senior");
        long normal    = validationEventRepository.countByPerfilClassificado("normal");
        long totalClassificados   = estudante + senior + normal;
        long totalNaoCategorizado = validationEventRepository.countByPerfilClassificado(NAO_CATEGORIZADO);
        return new DtoEstatisticasCategorizacao(totalClassificados, totalNaoCategorizado,
            estudante, senior, normal);
    }

    public List<MapeamentoTipologiaPerfil> getMappings() {
        return mappingRepository.findAll();
    }

    @Transactional
    public MapeamentoTipologiaPerfil createMapping(MapeamentoTipologiaPerfil mapping) {
        if (mappingRepository.existsByTipoTitulo(mapping.getTipoTitulo())) {
            throw new RuntimeException("Já existe um mapeamento para: " + mapping.getTipoTitulo());
        }
        mapping.setCreatedAt(OffsetDateTime.now());
        mapping.setUpdatedAt(OffsetDateTime.now());
        if (mapping.getUpdatedBy() == null) mapping.setUpdatedBy("api-rest");
        MapeamentoTipologiaPerfil saved = mappingRepository.saveAndFlush(mapping);
        registerAudit("CREATE", saved.getTipoTitulo(), null, saved.getPerfil(), saved.getUpdatedBy());
        validationEventRepository.updateProfileByTicketCode(saved.getTipoTitulo(), saved.getPerfil());
        return saved;
    }

    @Transactional
    public MapeamentoTipologiaPerfil updateMapping(Long id, MapeamentoTipologiaPerfil dados) {
        MapeamentoTipologiaPerfil existing = mappingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mapeamento não encontrado: " + id));
        String perfilAnterior = existing.getPerfil();
        existing.setTipoTitulo(dados.getTipoTitulo());
        existing.setPerfil(dados.getPerfil());
        existing.setUpdatedAt(OffsetDateTime.now());
        if (dados.getUpdatedBy() != null) existing.setUpdatedBy(dados.getUpdatedBy());
        MapeamentoTipologiaPerfil saved = mappingRepository.save(existing);
        registerAudit("UPDATE", saved.getTipoTitulo(), perfilAnterior, saved.getPerfil(), saved.getUpdatedBy());
        validationEventRepository.updateProfileByTicketCode(saved.getTipoTitulo(), saved.getPerfil());
        return saved;
    }

    @Transactional
    public void deleteMapping(Long id) {
        MapeamentoTipologiaPerfil existing = mappingRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Mapeamento não encontrado: " + id));
        registerAudit("DELETE", existing.getTipoTitulo(), existing.getPerfil(), null, "sistema");
        validationEventRepository.updateProfileByTicketCode(existing.getTipoTitulo(), NAO_CATEGORIZADO);
        mappingRepository.deleteById(id);
    }

    @Transactional
    public AuditoriaCategorizacao logAudit(AuditoriaCategorizacao audit) {
        if (audit.getTimestamp() == null) audit.setTimestamp(OffsetDateTime.now());
        return auditRepository.save(audit);
    }

    public List<EventoValidacao> getUncategorized(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null) {
            OffsetDateTime inicio = dataInicio.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime fim    = dataFim.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            return validationEventRepository
                .findByPerfilClassificadoAndTransactionDateTimeBetween(NAO_CATEGORIZADO, inicio, fim);
        }
        return validationEventRepository.findByPerfilClassificado(NAO_CATEGORIZADO);
    }

    @Transactional
    public void reprocess() { classify(); }

    @Transactional
    public void reset() {
        mappingRepository.deleteAll();
        auditRepository.deleteAll();
        validationEventRepository.resetAllClassifications(NAO_CATEGORIZADO);
    }

    @Transactional
    public void deleteUncategorized() {
        validationEventRepository.deleteByPerfilClassificado(NAO_CATEGORIZADO);
    }

    // -------------------------------------------------------------------------
    // Auxiliares
    // -------------------------------------------------------------------------

    private void registerUncategorizedEvent(EventoValidacao event, String motivo) {
        EventoNaoCategorizado enc = new EventoNaoCategorizado(
            event.getIngestionHash(),
            motivo,
            "PENDENTE",
            OffsetDateTime.now()
        );
        eventoNaoCategorizadoRepository.save(enc);
    }

    private Map<String, String> buildMappingCache() {
        Map<String, String> cache = new HashMap<>();
        for (MapeamentoTipologiaPerfil m : mappingRepository.findAll()) {
            cache.put(m.getTipoTitulo(), m.getPerfil());
        }
        return cache;
    }

    private void registerAudit(String acao, String tipoTitulo,
                               String perfilAnterior, String perfilNovo,
                               String utilizador) {
        AuditoriaCategorizacao audit = new AuditoriaCategorizacao();
        audit.setAcao(acao);
        audit.setTipoTitulo(tipoTitulo);
        audit.setPerfilAnterior(perfilAnterior);
        audit.setPerfilNovo(perfilNovo);
        audit.setTimestamp(OffsetDateTime.now());
        audit.setUtilizador(utilizador != null ? utilizador : "sistema");
        auditRepository.save(audit);
    }
}
