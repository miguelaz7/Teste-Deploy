package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioRota extends JpaRepository<Rota, Long> {
    boolean existsByRouteShortName(String routeShortName);
}
