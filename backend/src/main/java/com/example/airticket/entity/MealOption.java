package com.example.airticket.entity;

import javax.persistence.*;

@Entity
@Table(name = "MealOption")
public class MealOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MealId")
    public Integer mealId;

    @Column(name = "MealName", nullable = false, unique = true)
    public String mealName;

    @Column(name = "MealType", nullable = false)
    public String mealType;

    @Column(name = "IsAvailable", nullable = false)
    public Boolean isAvailable = true;

    @Column(name = "Description")
    public String description;
}
