package com.example.demo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BilheteRepository extends JpaRepository<Bilhete, Long>, JpaSpecificationExecutor<Bilhete> {
    Optional<Bilhete> findByCodigo(String codigo);
}
