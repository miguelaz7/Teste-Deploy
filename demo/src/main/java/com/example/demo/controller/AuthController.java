package com.example.demo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/profile")
    public Map<String, Object> profile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "name", jwt.getClaimAsString("name"),
            "email", jwt.getClaimAsString("email"),
            "sub", jwt.getSubject()
        );
    }

    @GetMapping("/public/hello")
    public String hello() {
        return "Endpoint público! Sem autenticação necessária.";
    }
}