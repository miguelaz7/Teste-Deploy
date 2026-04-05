package com.example.demo.repository;

import com.example.demo.model.FareCollectionSystem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FareCollectionSystemRepository extends JpaRepository<FareCollectionSystem, Long> {
    Optional<FareCollectionSystem> findBySystemCode(String systemCode);
}
