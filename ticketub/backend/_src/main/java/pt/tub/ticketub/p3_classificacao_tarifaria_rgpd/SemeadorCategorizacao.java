package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
public class SemeadorCategorizacao {

    private final RepositorioMapeamentoTipologiaPerfil repository;
    private final ControladorClassificacaoTarifaria controlador;

    public SemeadorCategorizacao(RepositorioMapeamentoTipologiaPerfil repository,
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
        MapeamentoTipologiaPerfil mapping = new MapeamentoTipologiaPerfil();
        mapping.setTipoTitulo(tipoTitulo);
        mapping.setPerfil(perfil);
        mapping.setCreatedAt(OffsetDateTime.now());
        mapping.setUpdatedAt(OffsetDateTime.now());
        mapping.setUpdatedBy("SYSTEM_SEEDER");
        repository.save(mapping);
    }
}
