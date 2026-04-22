package com.example.demo.service;

import com.example.demo.model.TipologiaPerfilMapping;
import com.example.demo.repository.TipologiaPerfilMappingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class CategorizationSeeder {

    private final TipologiaPerfilMappingRepository repository;
    private final CategorizationService categorizationService;

    public CategorizationSeeder(TipologiaPerfilMappingRepository repository,
                                CategorizationService categorizationService) {
        this.repository = repository;
        this.categorizationService = categorizationService;
    }

    // @PostConstruct
    public void seedMappings() {
        if (repository.count() > 0) return;

        // Códigos canónicos conforme TICKET_TYPES_CANONICOS em ValidationIngestionService
        createIfNotFound("MENSAL",          "normal");
        createIfNotFound("AVULSO",          "normal");
        createIfNotFound("PASSE_ESTUDANTE", "estudante");
        createIfNotFound("PASSE_SENIOR",    "senior");

        // Classificar todos os eventos pendentes com os mapeamentos recém-inseridos
        categorizationService.classify();
    }

    private void createIfNotFound(String tipoTitulo, String perfil) {
        if (repository.findByTipoTitulo(tipoTitulo).isPresent()) return;

        TipologiaPerfilMapping mapping = new TipologiaPerfilMapping();
        mapping.setTipoTitulo(tipoTitulo);
        mapping.setPerfil(perfil);
        mapping.setCreatedAt(OffsetDateTime.now());
        mapping.setUpdatedAt(OffsetDateTime.now());
        mapping.setUpdatedBy("SYSTEM_SEEDER");
        repository.save(mapping);
    }
}
