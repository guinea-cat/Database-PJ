package com.example.airticket.repository;

import com.example.airticket.entity.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Integer> {
    List<Flight> findByFlightDateBetween(LocalDate start, LocalDate end);
}
