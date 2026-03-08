package com.example.demo;

import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserAccountRepository repository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(UserAccountRepository repository) {
        this.repository = repository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        String firstName = normalizeName(request.getFirstName());
        String lastName = normalizeName(request.getLastName());
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();
        String confirmPassword = request.getConfirmPassword();

        if (firstName == null || lastName == null || email == null || password == null || password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Dados invalidos. Verifica nome, apelido e password (minimo 6 caracteres)."));
        }

        if (!password.equals(confirmPassword)) {
            return ResponseEntity.badRequest().body(Map.of("message", "As passwords nao coincidem."));
        }

        if (repository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Este email ja esta registado."));
        }

        String hashedPassword = passwordEncoder.encode(password);
        repository.save(new UserAccount(email, firstName, lastName, hashedPassword));

        return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse("Registo efetuado com sucesso."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        String email = normalizeEmail(request.getEmail());
        String password = request.getPassword();

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email e password sao obrigatorios."));
        }

        return repository.findByEmail(email)
            .filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
            .<ResponseEntity<?>>map(user -> ResponseEntity.ok(
                new AuthResponse("Login efetuado com sucesso.", user.getFirstName(), user.getLastName())
            ))
            .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Credenciais invalidas.")));
    }

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }

        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

}
