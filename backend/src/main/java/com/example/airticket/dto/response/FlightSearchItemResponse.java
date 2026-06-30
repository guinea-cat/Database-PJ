package com.example.airticket.dto.response;

import com.example.airticket.entity.FlightSegment;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    public BigDecimal originalFirstClassPrice;
    public BigDecimal originalEconomyPrice;
    public BigDecimal firstClassPrice;
    public BigDecimal economyPrice;
    public Boolean isSpecialOffer;
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
        response.originalFirstClassPrice = segment.firstClassPrice;
        response.originalEconomyPrice = segment.economyPrice;
        response.isSpecialOffer = Boolean.TRUE.equals(segment.isSpecialOffer);
        response.firstClassPrice = displayPrice(segment.firstClassPrice, segment.isSpecialOffer);
        response.economyPrice = displayPrice(segment.economyPrice, segment.isSpecialOffer);
        response.isAvailable = segment.firstClassRemainingSeats > 0 || segment.economyRemainingSeats > 0;
        return response;
    }

    private static BigDecimal displayPrice(BigDecimal originalPrice, Boolean isSpecialOffer) {
        BigDecimal price = Boolean.TRUE.equals(isSpecialOffer) ? originalPrice.multiply(new BigDecimal("0.50")) : originalPrice;
        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
