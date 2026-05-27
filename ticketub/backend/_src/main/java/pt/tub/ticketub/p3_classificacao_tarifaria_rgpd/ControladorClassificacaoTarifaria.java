package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import pt.tub.ticketub.p2_ingestao_processamento_dados.EventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.RepositorioEventoValidacao;
import pt.tub.ticketub.p2_ingestao_processamento_dados.ServicoSuspensaoLote;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RepositorioRegistoDataLakeNgsiLd;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RegistoDataLakeNgsiLd;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.RepositorioAlerta;
import pt.tub.ticketub.p7_monitorizacao_gestao_alertas.Alerta;

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
    private final RepositorioRegistoDataLakeNgsiLd dataLakeRepository;
    private final ServicoSuspensaoLote servicoSuspensaoLote;
    private final RepositorioAlerta alertaRepository;

    public ControladorClassificacaoTarifaria(
        RepositorioMapeamentoTipologiaPerfil mappingRepository,
        RepositorioAuditoriaCategorizacao auditRepository,
        RepositorioEventoValidacao validationEventRepository,
        RepositorioEventoNaoCategorizado eventoNaoCategorizadoRepository,
        RepositorioRegistoDataLakeNgsiLd dataLakeRepository,
        ServicoSuspensaoLote servicoSuspensaoLote,
        RepositorioAlerta alertaRepository
    ) {
        this.mappingRepository              = mappingRepository;
        this.auditRepository                = auditRepository;
        this.validationEventRepository      = validationEventRepository;
        this.eventoNaoCategorizadoRepository = eventoNaoCategorizadoRepository;
        this.dataLakeRepository             = dataLakeRepository;
        this.servicoSuspensaoLote           = servicoSuspensaoLote;
        this.alertaRepository               = alertaRepository;
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

            // 1. Detect PII (FA2)
            boolean hasPii = detectPII(ticketCode) || detectPII(event.getCardId()) || detectPII(event.getTicketId());
            if (hasPii) {
                event.setPiiDetected(true);
                event.setPerfilClassificado("SUSPENSO");

                // Find batchId from data lake
                String entityId = "urn:ngsi-ld:FareTransaction:" + event.getIngestionHash();
                String batchId = dataLakeRepository.findFirstByEntityId(entityId)
                    .map(RegistoDataLakeNgsiLd::getBatchId)
                    .orElse("DESCONHECIDO");

                // Suspend the batch
                if (!"DESCONHECIDO".equals(batchId)) {
                    servicoSuspensaoLote.suspenderLote(batchId);
                }

                // Notify DPO
                Alerta dpoAlert = new Alerta(
                    "DETECAO_PII",
                    "CRITICO",
                    event.getIngestionHash(),
                    "PII detectada no evento " + event.getIngestionHash() + " (Lote: " + batchId + "). Processamento do lote suspenso.",
                    OffsetDateTime.now()
                );
                alertaRepository.save(dpoAlert);

                registerAudit("PII_DETECTION", ticketCode != null ? ticketCode : "DESCONHECIDO", null, "SUSPENSO", "sistema");
                continue;
            }

            if (ticketCode == null) {
                event.setPerfilClassificado(NAO_CATEGORIZADO);
                registerUncategorizedEvent(event, "Tipo de título nulo");
                registerAudit("CLASSIFICATION_FAILED", "NULO", null, NAO_CATEGORIZADO, "sistema");
                continue;
            }

            String perfil = cache.get(ticketCode);
            if (perfil == null) {
                // Sem correspondência → vai para O0.3.3.d
                event.setPerfilClassificado(NAO_CATEGORIZADO);
                registerUncategorizedEvent(event, "Sem mapeamento para: " + ticketCode);
                registerAudit("CLASSIFICATION_FAILED", ticketCode, null, NAO_CATEGORIZADO, "sistema");
            } else {
                event.setPerfilClassificado(perfil);
                
                // Contextual geographic masking for sensitive categories (senior, estudante)
                if ("senior".equalsIgnoreCase(perfil) || "estudante".equalsIgnoreCase(perfil)) {
                    event.setOriginStop(null);
                    event.setMaskedFields(event.getMaskedFields() != null
                        ? event.getMaskedFields() + "originStop(SENSITIVE_MASK);"
                        : "originStop(SENSITIVE_MASK);");
                }

                registerAudit("CLASSIFICATION_SUCCESS", ticketCode, null, perfil, "sistema");
            }
        }

        validationEventRepository.saveAll(pendentes);
    }

    private boolean detectPII(String text) {
        if (text == null || text.isBlank()) return false;
        // Check for email pattern
        if (text.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,4}.*")) return true;
        // Check for 9-digit numbers (Portuguese NIF or phone numbers)
        if (text.matches(".*\\b[921][0-9]{8}\\b.*")) return true;
        // Check for international format phone numbers
        if (text.matches(".*\\+351\\s?[291][0-9]{2}\\s?[0-9]{3}\\s?[0-9]{3}.*")) return true;
        return false;
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
