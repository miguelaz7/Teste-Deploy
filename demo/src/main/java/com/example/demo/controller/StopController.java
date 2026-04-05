package com.example.demo.controller;

import com.example.demo.dto.StopLiveDataDto;
import com.example.demo.model.Stop;
import com.example.demo.repository.StopRepository;
import com.example.demo.service.StopLiveDataService;
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