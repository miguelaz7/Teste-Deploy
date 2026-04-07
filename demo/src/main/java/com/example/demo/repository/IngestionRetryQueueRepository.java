package com.example.demo.repository;

import com.example.demo.model.IngestionRetryQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface IngestionRetryQueueRepository extends JpaRepository<IngestionRetryQueue, Long> {
	List<IngestionRetryQueue> findTop50ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc(String status, OffsetDateTime reference);
}
