package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.Route;
import pt.tub.ticketub.p9_exportacao_interoperabilidade_externa.RouteRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RouteService {

    private final RouteRepository routeRepository;

    public RouteService(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Route> obterTodasRoitas() {
        return routeRepository.findAll();
    }
}






