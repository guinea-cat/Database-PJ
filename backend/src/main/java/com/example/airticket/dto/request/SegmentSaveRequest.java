package com.example.airticket.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SegmentSaveRequest {
    public Integer segmentId;
    public Integer flightId;
    public Integer originStopNo;
    public Integer destinationStopNo;
    public String originAirportCode;
    public String destinationAirportCode;
    public LocalDateTime plannedDepartureTime;
    public LocalDateTime plannedArrivalTime;
    public LocalDateTime actualDepartureTime;
    public LocalDateTime actualArrivalTime;
    public Integer delayMinutes;
    public String delayReason;
    public Integer firstClassRemainingSeats;
    public Integer economyRemainingSeats;
    public BigDecimal firstClassPrice;
    public BigDecimal economyPrice;
    public String remark;
}
