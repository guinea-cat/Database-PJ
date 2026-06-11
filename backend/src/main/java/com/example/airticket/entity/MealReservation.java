package com.example.airticket.entity;

import javax.persistence.*;

@Entity
@Table(name = "MealReservation")
public class MealReservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MealReservationId")
    public Integer mealReservationId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TicketId", nullable = false)
    public TicketSale ticket;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MealId", nullable = false)
    public MealOption meal;

    @Column(name = "Quantity", nullable = false)
    public Integer quantity = 1;
}
