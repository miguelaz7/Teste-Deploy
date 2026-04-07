package com.example.demo.repository;

import com.example.demo.model.CsvIngestionCursor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CsvIngestionCursorRepository extends JpaRepository<CsvIngestionCursor, Long> {
    Optional<CsvIngestionCursor> findByCsvPath(String csvPath);
}
