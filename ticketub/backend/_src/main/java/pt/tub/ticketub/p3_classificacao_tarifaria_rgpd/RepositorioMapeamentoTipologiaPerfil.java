package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RepositorioMapeamentoTipologiaPerfil extends JpaRepository<MapeamentoTipologiaPerfil, Long> {

    boolean existsByTipoTitulo(String tipoTitulo);
    Optional<MapeamentoTipologiaPerfil> findByTipoTitulo(String tipoTitulo);
}
