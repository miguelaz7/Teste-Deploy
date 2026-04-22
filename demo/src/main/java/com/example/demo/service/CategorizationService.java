package com.example.demo.service;

import com.example.demo.dto.CategorizationStatsDto;
import com.example.demo.model.CategorizationAudit;
import com.example.demo.model.TipologiaPerfilMapping;
import com.example.demo.model.ValidationEvent;
import com.example.demo.repository.CategorizationAuditRepository;
import com.example.demo.repository.TipologiaPerfilMappingRepository;
import com.example.demo.repository.ValidationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategorizationService {

    private static final String NAO_CATEGORIZADO = "nao_categorizado";

    private final TipologiaPerfilMappingRepository mappingRepository;
    private final CategorizationAuditRepository auditRepository;
    private final ValidationEventRepository validationEventRepository;

    public CategorizationService(TipologiaPerfilMappingRepository mappingRepository,
                                 CategorizationAuditRepository auditRepository,
                                 ValidationEventRepository validationEventRepository) {
        this.mappingRepository = mappingRepository;
        this.auditRepository = auditRepository;
        this.validationEventRepository = validationEventRepository;
    }

    // -------------------------------------------------------------------
    // classify() — core logic: varrer todos os eventos nao_categorizado
    // ou com perfil_classificado nulo e atribuir perfil via mapeamento
    // -------------------------------------------------------------------
    @Transactional
    public void classify() {
        List<ValidationEvent> pendentes = validationEventRepository
                .findByPerfilClassificadoIsNullOrPerfilClassificado(NAO_CATEGORIZADO);

        if (pendentes.isEmpty()) return;

        Map<String, String> cache = buildMappingCache();

        for (ValidationEvent event : pendentes) {
            String ticketCode = event.getTicketType() != null
                    ? event.getTicketType().getCode()
                    : null;
            if (ticketCode == null) {
                event.setPerfilClassificado(NAO_CATEGORIZADO);
                continue;
            }
            String perfil = cache.getOrDefault(ticketCode, NAO_CATEGORIZADO);
            event.setPerfilClassificado(perfil);
        }

        validationEventRepository.saveAll(pendentes);
    }

    // -------------------------------------------------------------------
    // getStats()
    // -------------------------------------------------------------------
    public CategorizationStatsDto getStats() {
        long estudante = validationEventRepository.countByPerfilClassificado("estudante");
        long senior    = validationEventRepository.countByPerfilClassificado("senior");
        long normal    = validationEventRepository.countByPerfilClassificado("normal");
        long totalClassificados    = estudante + senior + normal;
        long totalNaoCategorizado  = validationEventRepository.countByPerfilClassificado(NAO_CATEGORIZADO);

        return new CategorizationStatsDto(totalClassificados, totalNaoCategorizado,
                estudante, senior, normal);
    }

    // -------------------------------------------------------------------
    // getMappings()
    // -------------------------------------------------------------------
    public List<TipologiaPerfilMapping> getMappings() {
        return mappingRepository.findAll();
    }

    // -------------------------------------------------------------------
    // createMapping() — guarda, audita e reclassifica
    // -------------------------------------------------------------------
    @Transactional
    public TipologiaPerfilMapping createMapping(TipologiaPerfilMapping mapping) {
        if (mappingRepository.existsByTipoTitulo(mapping.getTipoTitulo())) {
            throw new RuntimeException("Já existe um mapeamento para este tipo de título: " + mapping.getTipoTitulo());
        }
        
        mapping.setCreatedAt(OffsetDateTime.now());
        mapping.setUpdatedAt(OffsetDateTime.now());
        if (mapping.getUpdatedBy() == null) mapping.setUpdatedBy("api-rest");

        TipologiaPerfilMapping saved = mappingRepository.saveAndFlush(mapping);

        registarAuditoria("CREATE", saved.getTipoTitulo(), null, saved.getPerfil(),
                saved.getUpdatedBy() != null ? saved.getUpdatedBy() : "sistema");

        // Sincronizar todos os eventos deste tipo com o novo perfil imediatamente
        updateEventsForTicketCode(saved.getTipoTitulo(), saved.getPerfil());
        
        return saved;
    }

    // -------------------------------------------------------------------
    // updateMapping() — edita, audita e reclassifica
    // -------------------------------------------------------------------
    @Transactional
    public TipologiaPerfilMapping updateMapping(Long id, TipologiaPerfilMapping dados) {
        TipologiaPerfilMapping existing = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapeamento não encontrado: " + id));

        String perfilAnterior = existing.getPerfil();
        existing.setTipoTitulo(dados.getTipoTitulo());
        existing.setPerfil(dados.getPerfil());
        existing.setUpdatedAt(OffsetDateTime.now());
        if (dados.getUpdatedBy() != null) {
            existing.setUpdatedBy(dados.getUpdatedBy());
        }
        TipologiaPerfilMapping saved = mappingRepository.save(existing);

        registarAuditoria("UPDATE", saved.getTipoTitulo(), perfilAnterior, saved.getPerfil(),
                saved.getUpdatedBy() != null ? saved.getUpdatedBy() : "sistema");

        // Sincronizar todos os eventos históricos deste tipo com o novo perfil
        updateEventsForTicketCode(saved.getTipoTitulo(), saved.getPerfil());
        
        return saved;
    }

    // -------------------------------------------------------------------
    // deleteMapping() — apaga e audita
    // -------------------------------------------------------------------
    @Transactional
    public void deleteMapping(Long id) {
        TipologiaPerfilMapping existing = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapeamento não encontrado: " + id));

        registarAuditoria("DELETE", existing.getTipoTitulo(), existing.getPerfil(), null, "sistema");
        
        // Resetar todos os eventos deste tipo para "nao_categorizado"
        updateEventsForTicketCode(existing.getTipoTitulo(), NAO_CATEGORIZADO);
        
        mappingRepository.deleteById(id);
    }

    // -------------------------------------------------------------------
    // logAudit() — recebe auditoria vinda do frontend
    // -------------------------------------------------------------------
    @Transactional
    public CategorizationAudit logAudit(CategorizationAudit audit) {
        if (audit.getTimestamp() == null) {
            audit.setTimestamp(OffsetDateTime.now());
        }
        return auditRepository.save(audit);
    }

    // -------------------------------------------------------------------
    // getUncategorized() — filtra por data (LocalDate → OffsetDateTime)
    // -------------------------------------------------------------------
    public List<ValidationEvent> getUncategorized(LocalDate dataInicio, LocalDate dataFim) {
        if (dataInicio != null && dataFim != null) {
            OffsetDateTime inicio = dataInicio.atStartOfDay().atOffset(ZoneOffset.UTC);
            OffsetDateTime fim    = dataFim.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
            return validationEventRepository
                    .findByPerfilClassificadoAndTransactionDateTimeBetween(NAO_CATEGORIZADO, inicio, fim);
        }
        return validationEventRepository.findByPerfilClassificado(NAO_CATEGORIZADO);
    }

    // -------------------------------------------------------------------
    // reprocess() — chama classify() em todos os nao_categorizado
    // -------------------------------------------------------------------
    @Transactional
    public void reset() {
        // Apagar todos os mapeamentos
        mappingRepository.deleteAll();
        
        // Apagar todas as auditorias
        auditRepository.deleteAll();
        
        // Resetar TODOS os eventos de validação para nao_categorizado usando query direta (muito mais rápido)
        validationEventRepository.resetAllClassifications(NAO_CATEGORIZADO);
    }

    @Transactional
    public void reprocess() {
        classify();
    }

    @Transactional
    public void deleteUncategorized() {
        validationEventRepository.deleteByPerfilClassificado(NAO_CATEGORIZADO);
    }

    // -------------------------------------------------------------------
    // Helpers internos
    // -------------------------------------------------------------------
    @Transactional
    protected void updateEventsForTicketCode(String ticketCode, String novoPerfil) {
        // Usar Query direta no repositório para performance e fiabilidade
        validationEventRepository.updateProfileByTicketCode(ticketCode, novoPerfil);
    }

    private Map<String, String> buildMappingCache() {
        List<TipologiaPerfilMapping> todos = mappingRepository.findAll();
        Map<String, String> cache = new HashMap<>();
        for (TipologiaPerfilMapping m : todos) {
            cache.put(m.getTipoTitulo(), m.getPerfil());
        }
        return cache;
    }

    private void registarAuditoria(String acao, String tipoTitulo,
                                    String perfilAnterior, String perfilNovo,
                                    String utilizador) {
        CategorizationAudit audit = new CategorizationAudit();
        audit.setAcao(acao);
        audit.setTipoTitulo(tipoTitulo);
        audit.setPerfilAnterior(perfilAnterior);
        audit.setPerfilNovo(perfilNovo);
        audit.setTimestamp(OffsetDateTime.now());
        audit.setUtilizador(utilizador);
        auditRepository.save(audit);
    }
}
