package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.FareCollectionSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareCollectionSystemRepository extends JpaRepository<FareCollectionSystem, Long> {
    Optional<FareCollectionSystem> findBySystemCode(String systemCode);
}






