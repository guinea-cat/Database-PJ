package com.example.airticket.entity;

import javax.persistence.*;

@Entity
@Table(name = "Aircraft")
public class Aircraft {
    @Id
    @Column(name = "AircraftRegNo")
    public String aircraftRegNo;

    @Column(name = "AircraftType", nullable = false)
    public String aircraftType;

    @Column(name = "Manufacturer", nullable = false)
    public String manufacturer;

    @Column(name = "TotalFirstClassSeats", nullable = false)
    public Integer totalFirstClassSeats;

    @Column(name = "TotalEconomySeats", nullable = false)
    public Integer totalEconomySeats;

    @Column(name = "Status", nullable = false)
    public String status;

    @Column(name = "Remark")
    public String remark;
}
