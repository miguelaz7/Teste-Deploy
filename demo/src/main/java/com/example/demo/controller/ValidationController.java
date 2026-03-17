package com.example.demo.controller;

import com.example.demo.repository.ValidationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/validations")
public class ValidationController {

    @Autowired
    ValidationRepository repository;

    @GetMapping("/count-by-type")
    public List<Object[]> countByType() {
        return repository.countByTicketType();
    }
}