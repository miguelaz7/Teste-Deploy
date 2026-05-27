package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ServicoRota {

    private final RepositorioRota routeRepository;

    public ServicoRota(RepositorioRota routeRepository) {
        this.routeRepository = routeRepository;
    }

    public List<Rota> getAllRoutes() {
        return routeRepository.findAll();
    }
}
