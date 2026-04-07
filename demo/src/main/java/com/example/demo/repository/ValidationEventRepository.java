package com.example.demo.repository;

import com.example.demo.model.ValidationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface ValidationEventRepository extends JpaRepository<ValidationEvent, Long> {
	boolean existsByIngestionHashAndIngestedAtAfter(String ingestionHash, java.time.OffsetDateTime reference);
	List<ValidationEvent> findByOriginStop_StopId(String stopId);
	long countByIngestedAtAfter(OffsetDateTime reference);
}