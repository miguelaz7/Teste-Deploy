package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class CategorizationSeeder {

    private final TipologiaPerfilMappingRepository repository;
    private final ControladorClassificacaoTarifaria controlador;

    public CategorizationSeeder(TipologiaPerfilMappingRepository repository,
                                ControladorClassificacaoTarifaria controlador) {
        this.repository  = repository;
        this.controlador = controlador;
    }

    @PostConstruct
    public void seedMappings() {
        if (repository.count() > 0) return;

        createIfNotFound("MENSAL",          "normal");
        createIfNotFound("AVULSO",          "normal");
        createIfNotFound("PASSE_ESTUDANTE", "estudante");
        createIfNotFound("PASSE_SENIOR",    "senior");

        controlador.classify();
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






