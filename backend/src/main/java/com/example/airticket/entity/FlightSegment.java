package com.example.airticket.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FlightSegment")
public class FlightSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SegmentId")
    public Integer segmentId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "FlightId", nullable = false)
    public Flight flight;

    @Column(name = "OriginStopNo", nullable = false)
    public Integer originStopNo;

    @Column(name = "DestinationStopNo", nullable = false)
    public Integer destinationStopNo;

    @Column(name = "OriginAirportCode", nullable = false)
    public String originAirportCode;

    @Column(name = "DestinationAirportCode", nullable = false)
    public String destinationAirportCode;

    @Column(name = "PlannedDepartureTime", nullable = false)
    public LocalDateTime plannedDepartureTime;

    @Column(name = "PlannedArrivalTime", nullable = false)
    public LocalDateTime plannedArrivalTime;

    @Column(name = "ActualDepartureTime")
    public LocalDateTime actualDepartureTime;

    @Column(name = "ActualArrivalTime")
    public LocalDateTime actualArrivalTime;

    @Column(name = "DelayMinutes")
    public Integer delayMinutes = 0;

    @Column(name = "DelayReason")
    public String delayReason;

    @Column(name = "FirstClassRemainingSeats", nullable = false)
    public Integer firstClassRemainingSeats;

    @Column(name = "EconomyRemainingSeats", nullable = false)
    public Integer economyRemainingSeats;

    @Column(name = "FirstClassPrice", nullable = false)
    public BigDecimal firstClassPrice;

    @Column(name = "EconomyPrice", nullable = false)
    public BigDecimal economyPrice;

    @Column(name = "Remark")
    public String remark;
}
