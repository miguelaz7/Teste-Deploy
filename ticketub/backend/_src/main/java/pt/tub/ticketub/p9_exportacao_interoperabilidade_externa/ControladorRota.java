package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/routes")
public class ControladorRota {

    private final ServicoRota routeService;

    public ControladorRota(ServicoRota routeService) {
        this.routeService = routeService;
    }

    @GetMapping
    public List<Rota> listRoutes() {
        return routeService.getAllRoutes();
    }
}
