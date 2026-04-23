package pt.tub.ticketub.p9_interoperabilidade;

import pt.tub.ticketub.p4_apresentacao.StopLiveDataDto;
import pt.tub.ticketub.p9_interoperabilidade.Stop;
import pt.tub.ticketub.p9_interoperabilidade.StopRepository;
import pt.tub.ticketub.p4_apresentacao.StopLiveDataService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopRepository stopRepository;
    private final StopLiveDataService stopLiveDataService;

    public StopController(StopRepository stopRepository, StopLiveDataService stopLiveDataService) {
        this.stopRepository = stopRepository;
        this.stopLiveDataService = stopLiveDataService;
    }

    @GetMapping
    public List<Stop> listStops() {
        return stopRepository.findAll();
    }

    @GetMapping("/{stopId}/live-data")
    public StopLiveDataDto obterLiveDataParagem(@PathVariable String stopId) {
        return stopLiveDataService.obterLiveDataPorParagem(stopId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paragem nao encontrada: " + stopId));
    }
}





