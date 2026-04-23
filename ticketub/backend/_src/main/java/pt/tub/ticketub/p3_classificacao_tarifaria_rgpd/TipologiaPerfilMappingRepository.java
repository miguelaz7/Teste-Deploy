package pt.tub.ticketub.p3_classificacao_tarifaria_rgpd;

import pt.tub.ticketub.p3_classificacao_tarifaria_rgpd.TipologiaPerfilMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipologiaPerfilMappingRepository extends JpaRepository<TipologiaPerfilMapping, Long> {

    boolean existsByTipoTitulo(String tipoTitulo);
    Optional<TipologiaPerfilMapping> findByTipoTitulo(String tipoTitulo);

}






