package com.example.demo.repository;

import com.example.demo.model.NgsiLdDataLakeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NgsiLdDataLakeRecordRepository extends JpaRepository<NgsiLdDataLakeRecord, Long> {
    long countByBatchId(String batchId);
}
