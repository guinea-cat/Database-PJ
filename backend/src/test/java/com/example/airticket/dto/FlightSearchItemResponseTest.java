package com.example.airticket.dto;

import com.example.airticket.dto.response.FlightSearchItemResponse;
import com.example.airticket.entity.Flight;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.enums.FlightStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FlightSearchItemResponseTest {
    @Test
    void fromMarksSpecialOfferAndShowsOriginalAndDiscountedPrices() {
        Flight flight = new Flight();
        flight.flightId = 21;
        flight.flightNumber = "MU2001";
        flight.flightDate = LocalDate.of(2026, 7, 1);
        flight.flightStatus = FlightStatus.NORMAL;
        flight.aircraftRegNo = "B-1001";
        flight.departureAirportCode = "PEK";
        flight.arrivalAirportCode = "SHA";

        FlightSegment segment = new FlightSegment();
        segment.segmentId = 99;
        segment.flight = flight;
        segment.originStopNo = 1;
        segment.destinationStopNo = 2;
        segment.originAirportCode = "PEK";
        segment.destinationAirportCode = "SHA";
        segment.plannedDepartureTime = LocalDateTime.of(2026, 7, 1, 8, 0);
        segment.plannedArrivalTime = LocalDateTime.of(2026, 7, 1, 10, 0);
        segment.firstClassRemainingSeats = 8;
        segment.economyRemainingSeats = 50;
        segment.firstClassPrice = new BigDecimal("1800.00");
        segment.economyPrice = new BigDecimal("1000.00");
        segment.isSpecialOffer = true;

        FlightSearchItemResponse response = FlightSearchItemResponse.from(segment);

        assertThat(response.isSpecialOffer).isTrue();
        assertThat(response.originalEconomyPrice).isEqualByComparingTo("1000.00");
        assertThat(response.originalFirstClassPrice).isEqualByComparingTo("1800.00");
        assertThat(response.economyPrice).isEqualByComparingTo("500.00");
        assertThat(response.firstClassPrice).isEqualByComparingTo("900.00");
    }
}
