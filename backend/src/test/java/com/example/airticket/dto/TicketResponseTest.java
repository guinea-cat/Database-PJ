package com.example.airticket.dto;

import com.example.airticket.dto.response.TicketResponse;
import com.example.airticket.entity.Flight;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.entity.TicketSale;
import com.example.airticket.entity.User;
import com.example.airticket.enums.CabinClass;
import com.example.airticket.enums.FlightStatus;
import com.example.airticket.enums.TicketStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TicketResponseTest {
    @Test
    void fromIncludesReadableFlightAndSegmentFieldsForOrderList() {
        Flight flight = new Flight();
        flight.flightId = 21;
        flight.flightNumber = "MU2001";
        flight.flightDate = LocalDate.of(2026, 7, 1);
        flight.flightStatus = FlightStatus.NORMAL;

        FlightSegment segment = new FlightSegment();
        segment.segmentId = 99;
        segment.originAirportCode = "PEK";
        segment.destinationAirportCode = "SHA";
        segment.plannedDepartureTime = LocalDateTime.of(2026, 7, 1, 8, 0);
        segment.plannedArrivalTime = LocalDateTime.of(2026, 7, 1, 10, 0);

        User user = new User();
        user.userId = 2;
        user.loginAccount = "passengerA";
        user.userName = "演示乘客A";

        TicketSale ticket = new TicketSale();
        ticket.ticketId = 10001;
        ticket.orderNo = "ORD1";
        ticket.user = user;
        ticket.flight = flight;
        ticket.segment = segment;
        ticket.ticketStatus = TicketStatus.PAID;
        ticket.cabinClass = CabinClass.ECONOMY;
        ticket.passengerName = "演示乘客";
        ticket.priceAmount = new BigDecimal("800.00");
        ticket.paymentAmount = new BigDecimal("800.00");

        TicketResponse response = TicketResponse.from(ticket);

        assertThat(response.loginAccount).isEqualTo("passengerA");
        assertThat(response.userName).isEqualTo("演示乘客A");
        assertThat(response.flightNumber).isEqualTo("MU2001");
        assertThat(response.flightDate).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(response.originAirportCode).isEqualTo("PEK");
        assertThat(response.destinationAirportCode).isEqualTo("SHA");
        assertThat(response.plannedDepartureTime).isEqualTo(LocalDateTime.of(2026, 7, 1, 8, 0));
        assertThat(response.plannedArrivalTime).isEqualTo(LocalDateTime.of(2026, 7, 1, 10, 0));
    }

    @Test
    void fromIncludesOriginalOrderNumberForChangeChainDisplay() {
        TicketSale originalTicket = new TicketSale();
        originalTicket.ticketId = 10001;
        originalTicket.orderNo = "TS202607010001";

        TicketSale changeTicket = new TicketSale();
        changeTicket.ticketId = 10002;
        changeTicket.orderNo = "TS202607010002";
        changeTicket.ticketStatus = TicketStatus.PAID;
        changeTicket.cabinClass = CabinClass.ECONOMY;
        changeTicket.priceAmount = new BigDecimal("800.00");
        changeTicket.paymentAmount = new BigDecimal("100.00");
        changeTicket.originalTicket = originalTicket;

        TicketResponse response = TicketResponse.from(changeTicket);

        assertThat(response.originalTicketId).isEqualTo(10001);
        assertThat(response.originalOrderNo).isEqualTo("TS202607010001");
    }
}
