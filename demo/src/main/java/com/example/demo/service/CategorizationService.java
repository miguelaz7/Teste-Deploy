package com.example.demo.service;

import com.example.demo.model.CategorizationAudit;
import com.example.demo.model.TipologiaPerfilMapping;
import com.example.demo.model.ValidationEvent;
import com.example.demo.repository.CategorizationAuditRepository;
import com.example.demo.repository.TipologiaPerfilMappingRepository;
import com.example.demo.repository.ValidationEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CategorizationService {

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

    public List<TipologiaPerfilMapping> getAllMappings() {
        return mappingRepository.findAll();
    }

    @Transactional
    public TipologiaPerfilMapping createMapping(TipologiaPerfilMapping mapping) {
        mapping.setCreatedAt(OffsetDateTime.now());
        mapping.setUpdatedAt(OffsetDateTime.now());
        return mappingRepository.save(mapping);
    }

    @Transactional
    public TipologiaPerfilMapping updateMapping(Long id, TipologiaPerfilMapping updatedMapping) {
        TipologiaPerfilMapping existing = mappingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mapping not found"));
        
        existing.setTipoTitulo(updatedMapping.getTipoTitulo());
        existing.setPerfil(updatedMapping.getPerfil());
        existing.setUpdatedAt(OffsetDateTime.now());
        if (updatedMapping.getUpdatedBy() != null) {
            existing.setUpdatedBy(updatedMapping.getUpdatedBy());
        }
        return mappingRepository.save(existing);
    }

    @Transactional
    public void deleteMapping(Long id) {
        mappingRepository.deleteById(id);
    }

    @Transactional
    public CategorizationAudit logAudit(CategorizationAudit audit) {
        if (audit.getTimestamp() == null) {
            audit.setTimestamp(OffsetDateTime.now());
        }
        return auditRepository.save(audit);
    }

    public Map<String, Object> getStats() {
        long totalClassificados = validationEventRepository.countByPerfilClassificado("estudante") +
                                  validationEventRepository.countByPerfilClassificado("senior") +
                                  validationEventRepository.countByPerfilClassificado("normal");
        
        long totalNaoCategorizado = validationEventRepository.countByPerfilClassificado("nao_categorizado");
        long estudante = validationEventRepository.countByPerfilClassificado("estudante");
        long senior = validationEventRepository.countByPerfilClassificado("senior");
        long normal = validationEventRepository.countByPerfilClassificado("normal");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalClassificados", totalClassificados);
        stats.put("totalNaoCategorizado", totalNaoCategorizado);
        stats.put("estudante", estudante);
        stats.put("senior", senior);
        stats.put("normal", normal);
        return stats;
    }

    public List<ValidationEvent> getUncategorized(OffsetDateTime startDate, OffsetDateTime endDate) {
        if (startDate != null && endDate != null) {
            return validationEventRepository.findByPerfilClassificadoAndTransactionDateTimeBetween("nao_categorizado", startDate, endDate);
        }
        return validationEventRepository.findByPerfilClassificado("nao_categorizado");
    }

    @Transactional
    public void reprocess() {
        List<ValidationEvent> uncategorized = validationEventRepository.findByPerfilClassificadoAndPiiDetectedFalse("nao_categorizado");
        if (uncategorized.isEmpty()) return;

        List<TipologiaPerfilMapping> mappings = mappingRepository.findAll();
        Map<String, String> mappingMap = new HashMap<>();
        for (TipologiaPerfilMapping m : mappings) {
            mappingMap.put(m.getTipoTitulo(), m.getPerfil());
        }

        for (ValidationEvent event : uncategorized) {
            String ticketCode = event.getTicketType() != null ? event.getTicketType().getCode() : null;
            if (ticketCode == null) continue;

            if (isPiiDetected(event)) {
                event.setPiiDetected(true);
                continue;
            }

            if (mappingMap.containsKey(ticketCode)) {
                String novoPerfil = mappingMap.get(ticketCode);
                event.setPerfilClassificado(novoPerfil);

                CategorizationAudit audit = new CategorizationAudit();
                audit.setAcao("UPDATE_REPROCESS");
                audit.setTipoTitulo(ticketCode);
                audit.setPerfilAnterior("nao_categorizado");
                audit.setPerfilNovo(novoPerfil);
                audit.setTimestamp(OffsetDateTime.now());
                audit.setUtilizador("SYSTEM");
                auditRepository.save(audit);
            }
        }
        validationEventRepository.saveAll(uncategorized);
    }

    private boolean isPiiDetected(ValidationEvent event) {
        String card = event.getCardId();
        if (card == null) return false;
        // Simple logic for PII detection mock: 
        // If it looks like an email or a very short non-hashed term
        if (card.contains("@") && card.contains(".")) return true;
        if (card.length() < 5) return true; // Just as an example rule
        return false;
    }

    public Map<String, String> getMappingCache() {
        List<TipologiaPerfilMapping> mappings = mappingRepository.findAll();
        Map<String, String> mappingMap = new HashMap<>();
        for (TipologiaPerfilMapping m : mappings) {
            mappingMap.put(m.getTipoTitulo(), m.getPerfil());
        }
        return mappingMap;
    }

    public void classifyEventOnIngestion(ValidationEvent event, Map<String, String> mappingCache) {
        String ticketCode = event.getTicketType() != null ? event.getTicketType().getCode() : null;
        if (ticketCode == null) return;

        if (isPiiDetected(event)) {
            event.setPiiDetected(true);
            event.setPerfilClassificado("nao_categorizado");
            return;
        }

        if (mappingCache.containsKey(ticketCode)) {
            event.setPerfilClassificado(mappingCache.get(ticketCode));
        } else {
            event.setPerfilClassificado("nao_categorizado");
        }
    }
}
