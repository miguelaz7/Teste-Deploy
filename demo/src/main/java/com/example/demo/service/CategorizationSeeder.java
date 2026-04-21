package com.example.demo.service;

import com.example.demo.model.TipologiaPerfilMapping;
import com.example.demo.repository.TipologiaPerfilMappingRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class CategorizationSeeder {

    private final TipologiaPerfilMappingRepository repository;

    public CategorizationSeeder(TipologiaPerfilMappingRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void seedMappings() {
        if (repository.count() == 0) {
            System.out.println("Seeding Categorization Mappings...");

            // MONTHLY_PASS → normal, SENIOR_PASS → senior, SINGLE_TICKET → normal, PASSE_ESTUDANTE → estudante, AVULSO → normal
            createIfNotFound("MONTHLY_PASS", "normal");
            createIfNotFound("SENIOR_PASS", "senior");
            createIfNotFound("SINGLE_TICKET", "normal");
            createIfNotFound("PASSE_ESTUDANTE", "estudante");
            createIfNotFound("AVULSO", "normal");
            
            System.out.println("Categorization Mappings seeded.");
        }
    }

    private void createIfNotFound(String tipoTitulo, String perfil) {
        if (repository.findByTipoTitulo(tipoTitulo).isEmpty()) {
            TipologiaPerfilMapping mapping = new TipologiaPerfilMapping();
            mapping.setTipoTitulo(tipoTitulo);
            mapping.setPerfil(perfil);
            mapping.setCreatedAt(OffsetDateTime.now());
            mapping.setUpdatedAt(OffsetDateTime.now());
            mapping.setUpdatedBy("SYSTEM_SEEDER");
            repository.save(mapping);
        }
    }
}
