package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
	boolean existsByRouteShortName(String routeShortName);
}






