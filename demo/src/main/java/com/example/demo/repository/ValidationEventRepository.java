package com.example.demo.repository;

import com.example.demo.model.ValidationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidationEventRepository extends JpaRepository<ValidationEvent, Long> {
	boolean existsByIngestionHashAndIngestedAtAfter(String ingestionHash, java.time.OffsetDateTime reference);
}