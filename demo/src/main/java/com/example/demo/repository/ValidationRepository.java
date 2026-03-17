package com.example.demo.repository;

import com.example.demo.model.Validation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ValidationRepository extends JpaRepository<Validation, Long> {

@Query("SELECT v.ticketType, COUNT(v) FROM Validation v GROUP BY v.ticketType")
List<Object[]> countByTicketType();

}