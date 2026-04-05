package com.example.demo.repository;

import com.example.demo.model.IngestionRetryQueue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IngestionRetryQueueRepository extends JpaRepository<IngestionRetryQueue, Long> {
}
