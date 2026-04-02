package com.example.demo.controller;

import com.example.demo.model.Stop;
import com.example.demo.repository.StopRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stops")
public class StopController {

    private final StopRepository stopRepository;

    public StopController(StopRepository stopRepository) {
        this.stopRepository = stopRepository;
    }

    @GetMapping
    public List<Stop> listStops() {
        return stopRepository.findAll();
    }
}