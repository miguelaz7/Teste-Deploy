package com.example.demo.repository;

import com.example.demo.model.StopTimes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StopTimesRepository extends JpaRepository<StopTimes, Long> {
	boolean existsByTripIdAndStopId(String tripId, String stopId);
}
