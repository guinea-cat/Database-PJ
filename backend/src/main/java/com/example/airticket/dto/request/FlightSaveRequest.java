package com.example.airticket.dto.request;

import java.time.LocalDate;

public class FlightSaveRequest {
    public Integer flightId;
    public String flightNumber;
    public LocalDate flightDate;
    public String aircraftRegNo;
    public String flightStatus;
    public String departureAirportCode;
    public String arrivalAirportCode;
    public String remark;
}
