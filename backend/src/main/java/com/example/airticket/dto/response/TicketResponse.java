package com.example.airticket.dto.response;

import com.example.airticket.entity.TicketSale;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class TicketResponse {
    public Integer ticketId;
    public String orderNo;
    public String ticketStatus;
    public Integer userId;
    public Integer flightId;
    public Integer segmentId;
    public String flightNumber;
    public LocalDate flightDate;
    public String originAirportCode;
    public String destinationAirportCode;
    public LocalDateTime plannedDepartureTime;
    public LocalDateTime plannedArrivalTime;
    public String cabinClass;
    public String passengerName;
    public BigDecimal priceAmount;
    public BigDecimal paymentAmount;
    public Integer originalTicketId;
    public String changeReason;
    public LocalDateTime bookedAt;
    public LocalDateTime paidAt;
    public LocalDateTime issuedAt;
    public LocalDateTime changedAt;
    public LocalDateTime refundedAt;
    public LocalDateTime expiredAt;
    public String remark;

    public static TicketResponse from(TicketSale ticket) {
        TicketResponse response = new TicketResponse();
        response.ticketId = ticket.ticketId;
        response.orderNo = ticket.orderNo;
        response.ticketStatus = ticket.ticketStatus.name();
        response.userId = ticket.user == null ? null : ticket.user.userId;
        response.flightId = ticket.flight == null ? null : ticket.flight.flightId;
        response.segmentId = ticket.segment == null ? null : ticket.segment.segmentId;
        response.flightNumber = ticket.flight == null ? null : ticket.flight.flightNumber;
        response.flightDate = ticket.flight == null ? null : ticket.flight.flightDate;
        response.originAirportCode = ticket.segment == null ? null : ticket.segment.originAirportCode;
        response.destinationAirportCode = ticket.segment == null ? null : ticket.segment.destinationAirportCode;
        response.plannedDepartureTime = ticket.segment == null ? null : ticket.segment.plannedDepartureTime;
        response.plannedArrivalTime = ticket.segment == null ? null : ticket.segment.plannedArrivalTime;
        response.cabinClass = ticket.cabinClass == null ? null : ticket.cabinClass.name();
        response.passengerName = ticket.passengerName;
        response.priceAmount = ticket.priceAmount;
        response.paymentAmount = ticket.paymentAmount;
        response.originalTicketId = ticket.originalTicket == null ? null : ticket.originalTicket.ticketId;
        response.changeReason = ticket.changeReason;
        response.bookedAt = ticket.bookedAt;
        response.paidAt = ticket.paidAt;
        response.issuedAt = ticket.issuedAt;
        response.changedAt = ticket.changedAt;
        response.refundedAt = ticket.refundedAt;
        response.expiredAt = ticket.expiredAt;
        response.remark = ticket.remark;
        return response;
    }
}
