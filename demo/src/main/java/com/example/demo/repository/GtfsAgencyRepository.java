package com.example.demo.repository;

import com.example.demo.model.GtfsAgency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface GtfsAgencyRepository extends JpaRepository<GtfsAgency, String> {
}