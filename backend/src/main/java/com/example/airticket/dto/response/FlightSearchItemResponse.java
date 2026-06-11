package com.example.airticket.dto.response;

import com.example.airticket.entity.FlightSegment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class FlightSearchItemResponse {
    public Integer flightId;
    public String flightNumber;
    public LocalDate flightDate;
    public String flightStatus;
    public String aircraftRegNo;
    public String departureAirportCode;
    public String arrivalAirportCode;
    public Integer segmentId;
    public Integer originStopNo;
    public Integer destinationStopNo;
    public String originAirportCode;
    public String destinationAirportCode;
    public LocalDateTime plannedDepartureTime;
    public LocalDateTime plannedArrivalTime;
    public Integer firstClassRemainingSeats;
    public Integer economyRemainingSeats;
    public BigDecimal firstClassPrice;
    public BigDecimal economyPrice;
    public Boolean isAvailable;

    public static FlightSearchItemResponse from(FlightSegment segment) {
        FlightSearchItemResponse response = new FlightSearchItemResponse();
        response.flightId = segment.flight.flightId;
        response.flightNumber = segment.flight.flightNumber;
        response.flightDate = segment.flight.flightDate;
        response.flightStatus = segment.flight.flightStatus.name();
        response.aircraftRegNo = segment.flight.aircraftRegNo;
        response.departureAirportCode = segment.flight.departureAirportCode;
        response.arrivalAirportCode = segment.flight.arrivalAirportCode;
        response.segmentId = segment.segmentId;
        response.originStopNo = segment.originStopNo;
        response.destinationStopNo = segment.destinationStopNo;
        response.originAirportCode = segment.originAirportCode;
        response.destinationAirportCode = segment.destinationAirportCode;
        response.plannedDepartureTime = segment.plannedDepartureTime;
        response.plannedArrivalTime = segment.plannedArrivalTime;
        response.firstClassRemainingSeats = segment.firstClassRemainingSeats;
        response.economyRemainingSeats = segment.economyRemainingSeats;
        response.firstClassPrice = segment.firstClassPrice;
        response.economyPrice = segment.economyPrice;
        response.isAvailable = segment.firstClassRemainingSeats > 0 || segment.economyRemainingSeats > 0;
        return response;
    }
}
