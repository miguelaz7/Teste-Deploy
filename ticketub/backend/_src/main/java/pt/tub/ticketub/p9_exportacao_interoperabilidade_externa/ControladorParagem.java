package pt.tub.ticketub.p9_exportacao_interoperabilidade_externa;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class ControladorParagem {

    private final RepositorioParagem stopRepository;

    public ControladorParagem(RepositorioParagem stopRepository) {
        this.stopRepository = stopRepository;
    }

    @GetMapping
    public List<Paragem> listStops() {
        return stopRepository.findAll();
    }
}
