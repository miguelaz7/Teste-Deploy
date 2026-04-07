package com.example.demo.repository;

import com.example.demo.model.ValidationQuarantine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface ValidationQuarantineRepository extends JpaRepository<ValidationQuarantine, Long> {
	long countByCreatedAtAfter(OffsetDateTime reference);
}
