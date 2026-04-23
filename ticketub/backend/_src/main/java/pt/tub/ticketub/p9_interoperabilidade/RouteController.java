package pt.tub.ticketub.p9_interoperabilidade;

import pt.tub.ticketub.p9_interoperabilidade.Route;
import pt.tub.ticketub.p9_interoperabilidade.RouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<Route> listRoutes() {
        return routeService.obterTodasRoitas();
    }
}





