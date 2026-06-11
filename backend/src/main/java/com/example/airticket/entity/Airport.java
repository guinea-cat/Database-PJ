package com.example.airticket.entity;

import javax.persistence.*;

@Entity
@Table(name = "Airport")
public class Airport {
    @Id
    @Column(name = "AirportCode")
    public String airportCode;

    @Column(name = "AirportName", nullable = false)
    public String airportName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "CityId", nullable = false)
    public City city;

    @Column(name = "IsInternational", nullable = false)
    public Boolean isInternational = false;
}
