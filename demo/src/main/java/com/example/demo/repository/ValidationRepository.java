package com.example.demo.repository;

import com.example.demo.model.Validation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ValidationRepository extends JpaRepository<Validation, Long> {

@Query("SELECT v.ticketType, COUNT(v) FROM Validation v GROUP BY v.ticketType")
List<Object[]> countByTicketType();

@Query(value = """
		SELECT
			stop_id,
			stop_name,
			DATE_FORMAT(`timestamp`, '%Y-%m-%d %H:00') AS time_gap,
			COUNT(*) AS total_validations
		FROM validations
		GROUP BY stop_id, stop_name, DATE_FORMAT(`timestamp`, '%Y-%m-%d %H:00')
		ORDER BY total_validations DESC
		LIMIT 1
		""", nativeQuery = true)
List<Object[]> findPeakAfluencia();

long countByIsValidFalse();

long countByStopId(String stopId);

}