package com.example.airticket.entity;

import javax.persistence.*;

@Entity
@Table(name = "City")
public class City {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "CityId")
    public Integer cityId;

    @Column(name = "CityName", nullable = false, unique = true)
    public String cityName;

    @Column(name = "CityCode", nullable = false)
    public String cityCode;

    @Column(name = "Country", nullable = false)
    public String country;
}
