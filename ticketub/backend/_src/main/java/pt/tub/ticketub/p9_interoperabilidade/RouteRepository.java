package pt.tub.ticketub.p9_interoperabilidade;

import pt.tub.ticketub.p9_interoperabilidade.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Long> {
	boolean existsByRouteShortName(String routeShortName);
}






