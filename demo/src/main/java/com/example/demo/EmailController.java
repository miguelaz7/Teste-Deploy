package com.example.demo;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/emails")
@CrossOrigin(origins = "*")
public class EmailController {

    private final EmailRepository repository;

    public EmailController(EmailRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public Email saveEmail(@RequestBody Email email) {
        return repository.save(email);
    }
}