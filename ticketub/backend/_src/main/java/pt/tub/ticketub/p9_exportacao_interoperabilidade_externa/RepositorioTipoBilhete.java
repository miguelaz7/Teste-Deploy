package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RepositorioTipoBilhete extends JpaRepository<TipoBilhete, Long> {
    Optional<TipoBilhete> findByCode(String code);
}
