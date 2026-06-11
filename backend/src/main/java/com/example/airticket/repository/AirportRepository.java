package com.example.airticket.repository;

import com.example.airticket.entity.Airport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AirportRepository extends JpaRepository<Airport, String> {
    List<Airport> findByCityCityId(Integer cityId);
}
