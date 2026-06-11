package com.example.airticket.entity;

import com.example.airticket.enums.FlightStatus;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "Flight")
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "FlightId")
    public Integer flightId;

    @Column(name = "FlightNumber", nullable = false)
    public String flightNumber;

    @Column(name = "FlightDate", nullable = false)
    public LocalDate flightDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "AircraftRegNo")
    public Aircraft aircraft;

    @Column(name = "AircraftRegNo", insertable = false, updatable = false)
    public String aircraftRegNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "FlightStatus", nullable = false)
    public FlightStatus flightStatus;

    @Column(name = "DepartureAirportCode", nullable = false)
    public String departureAirportCode;

    @Column(name = "ArrivalAirportCode", nullable = false)
    public String arrivalAirportCode;

    @Column(name = "Remark")
    public String remark;
}
