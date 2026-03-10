package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ViagemRepository extends JpaRepository<Viagem, Long> {
    Optional<Viagem> findByViagemId(String viagemId);
}
