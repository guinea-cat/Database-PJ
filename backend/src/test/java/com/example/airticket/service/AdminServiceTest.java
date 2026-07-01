package com.example.airticket.service;

import com.example.airticket.dto.request.SegmentSaveRequest;
import com.example.airticket.entity.Airport;
import com.example.airticket.entity.Flight;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.entity.TicketSale;
import com.example.airticket.entity.User;
import com.example.airticket.enums.CabinClass;
import com.example.airticket.enums.FlightStatus;
import com.example.airticket.enums.MemberLevel;
import com.example.airticket.enums.TicketStatus;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.AircraftRepository;
import com.example.airticket.repository.AirportRepository;
import com.example.airticket.repository.CityRepository;
import com.example.airticket.repository.FlightRepository;
import com.example.airticket.repository.FlightSegmentRepository;
import com.example.airticket.repository.MealOptionRepository;
import com.example.airticket.repository.TicketSaleRepository;
import com.example.airticket.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminServiceTest {
    private FlightRepository flightRepository;
    private FlightSegmentRepository segmentRepository;
    private AirportRepository airportRepository;
    private TicketSaleRepository ticketRepository;
    private AdminService adminService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = mock(UserRepository.class);
        CityRepository cityRepository = mock(CityRepository.class);
        airportRepository = mock(AirportRepository.class);
        AircraftRepository aircraftRepository = mock(AircraftRepository.class);
        flightRepository = mock(FlightRepository.class);
        segmentRepository = mock(FlightSegmentRepository.class);
        MealOptionRepository mealRepository = mock(MealOptionRepository.class);
        ticketRepository = mock(TicketSaleRepository.class);
        adminService = new AdminService(userRepository, cityRepository, airportRepository, aircraftRepository,
                flightRepository, segmentRepository, mealRepository, ticketRepository);
    }

    @Test
    void enableFlightRestoresDisabledFlightToNormal() {
        Flight flight = new Flight();
        flight.flightId = 10;
        flight.flightStatus = FlightStatus.DISABLED;
        when(flightRepository.findById(10)).thenReturn(Optional.of(flight));

        Flight enabled = adminService.enableFlight(10);

        assertThat(enabled.flightStatus).isEqualTo(FlightStatus.NORMAL);
    }

    @Test
    void disableFlightAutoRefundsPaidTicketsAndClosesPendingTickets() {
        Flight flight = new Flight();
        flight.flightId = 10;
        flight.flightStatus = FlightStatus.NORMAL;
        User user = new User();
        user.userId = 1;
        user.points = 100;
        user.memberLevel = MemberLevel.NORMAL;
        FlightSegment paidSegment = segmentForTicket(20, flight, 4, 8);
        FlightSegment pendingSegment = segmentForTicket(21, flight, 2, 6);
        TicketSale paid = ticket(100, user, flight, paidSegment, TicketStatus.PAID, CabinClass.ECONOMY);
        TicketSale pending = ticket(101, user, flight, pendingSegment, TicketStatus.PENDING_PAYMENT, CabinClass.FIRST_CLASS);

        when(flightRepository.findById(10)).thenReturn(Optional.of(flight));
        when(ticketRepository.findByFlightFlightIdAndTicketStatusIn(eq(10), any()))
                .thenReturn(List.of(paid, pending));

        adminService.disableFlight(10);

        assertThat(flight.flightStatus).isEqualTo(FlightStatus.DISABLED);
        assertThat(paid.ticketStatus).isEqualTo(TicketStatus.REFUND_SUCCESS);
        assertThat(paid.refundedAt).isNotNull();
        assertThat(paid.remark).isEqualTo("航班已停用，系统自动退款");
        assertThat(paidSegment.economyRemainingSeats).isEqualTo(9);
        assertThat(user.points).isZero();
        assertThat(pending.ticketStatus).isEqualTo(TicketStatus.EXPIRED);
        assertThat(pending.remark).isEqualTo("航班已停用，订单自动关闭");
        assertThat(pendingSegment.firstClassRemainingSeats).isEqualTo(3);
        verify(ticketRepository).findByFlightFlightIdAndTicketStatusIn(eq(10), any());
    }

    @Test
    void saveSegmentRejectsDuplicateRouteBeforeDatabaseUniqueConstraint() {
        Flight flight = new Flight();
        flight.flightId = 21;
        FlightSegment existing = new FlightSegment();
        existing.segmentId = 100;
        SegmentSaveRequest request = validSegmentRequest();

        when(flightRepository.findById(21)).thenReturn(Optional.of(flight));
        when(segmentRepository.findByFlightFlightIdAndOriginStopNoAndDestinationStopNo(21, 1, 2))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminService.saveSegment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("航段站序已存在");
    }

    @Test
    void saveSegmentRejectsUnknownAirportBeforeForeignKeyFailure() {
        Flight flight = new Flight();
        flight.flightId = 21;
        SegmentSaveRequest request = validSegmentRequest();

        when(flightRepository.findById(21)).thenReturn(Optional.of(flight));
        when(segmentRepository.findByFlightFlightIdAndOriginStopNoAndDestinationStopNo(21, 1, 2))
                .thenReturn(Optional.empty());
        when(airportRepository.findById("PEK")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.saveSegment(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("起飞机场不存在");
    }

    @Test
    void saveSegmentAllowsEditingTheSameSegmentRoute() {
        Flight flight = new Flight();
        flight.flightId = 21;
        FlightSegment existing = new FlightSegment();
        existing.segmentId = 100;
        SegmentSaveRequest request = validSegmentRequest();
        request.segmentId = 100;

        when(flightRepository.findById(21)).thenReturn(Optional.of(flight));
        when(segmentRepository.findById(100)).thenReturn(Optional.of(existing));
        when(segmentRepository.findByFlightFlightIdAndOriginStopNoAndDestinationStopNo(21, 1, 2))
                .thenReturn(Optional.of(existing));
        when(airportRepository.findById("PEK")).thenReturn(Optional.of(new Airport()));
        when(airportRepository.findById("SHA")).thenReturn(Optional.of(new Airport()));
        when(segmentRepository.save(any(FlightSegment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightSegment saved = adminService.saveSegment(request);

        assertThat(saved.segmentId).isEqualTo(100);
        assertThat(saved.originAirportCode).isEqualTo("PEK");
        assertThat(saved.destinationAirportCode).isEqualTo("SHA");
    }

    @Test
    void saveSegmentPersistsSpecialOfferFlag() {
        Flight flight = new Flight();
        flight.flightId = 21;
        SegmentSaveRequest request = validSegmentRequest();
        request.isSpecialOffer = true;

        when(flightRepository.findById(21)).thenReturn(Optional.of(flight));
        when(segmentRepository.findByFlightFlightIdAndOriginStopNoAndDestinationStopNo(21, 1, 2))
                .thenReturn(Optional.empty());
        when(airportRepository.findById("PEK")).thenReturn(Optional.of(new Airport()));
        when(airportRepository.findById("SHA")).thenReturn(Optional.of(new Airport()));
        when(segmentRepository.save(any(FlightSegment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FlightSegment saved = adminService.saveSegment(request);

        assertThat(saved.isSpecialOffer).isTrue();
    }

    private SegmentSaveRequest validSegmentRequest() {
        SegmentSaveRequest request = new SegmentSaveRequest();
        request.flightId = 21;
        request.originStopNo = 1;
        request.destinationStopNo = 2;
        request.originAirportCode = "PEK";
        request.destinationAirportCode = "SHA";
        request.plannedDepartureTime = LocalDateTime.of(2026, 7, 1, 8, 0);
        request.plannedArrivalTime = LocalDateTime.of(2026, 7, 1, 10, 0);
        request.firstClassRemainingSeats = 8;
        request.economyRemainingSeats = 50;
        request.firstClassPrice = new BigDecimal("1500.00");
        request.economyPrice = new BigDecimal("800.00");
        return request;
    }

    private FlightSegment segmentForTicket(int id, Flight flight, int firstClassSeats, int economySeats) {
        FlightSegment segment = new FlightSegment();
        segment.segmentId = id;
        segment.flight = flight;
        segment.firstClassRemainingSeats = firstClassSeats;
        segment.economyRemainingSeats = economySeats;
        return segment;
    }

    private TicketSale ticket(int id, User user, Flight flight, FlightSegment segment, TicketStatus status, CabinClass cabinClass) {
        TicketSale ticket = new TicketSale();
        ticket.ticketId = id;
        ticket.user = user;
        ticket.flight = flight;
        ticket.segment = segment;
        ticket.ticketStatus = status;
        ticket.cabinClass = cabinClass;
        return ticket;
    }
}
