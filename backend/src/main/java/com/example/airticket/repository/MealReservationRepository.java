package com.example.airticket.repository;

import com.example.airticket.entity.MealReservation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MealReservationRepository extends JpaRepository<MealReservation, Integer> {
}
