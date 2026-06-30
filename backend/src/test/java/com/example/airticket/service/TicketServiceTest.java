package com.example.airticket.service;

import com.example.airticket.dto.request.ChangeApplyRequest;
import com.example.airticket.dto.request.CreateTicketRequest;
import com.example.airticket.dto.request.PayTicketRequest;
import com.example.airticket.entity.Flight;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.entity.MealOption;
import com.example.airticket.entity.MealReservation;
import com.example.airticket.entity.TicketSale;
import com.example.airticket.entity.User;
import com.example.airticket.enums.CabinClass;
import com.example.airticket.enums.FlightStatus;
import com.example.airticket.enums.MemberLevel;
import com.example.airticket.enums.TicketStatus;
import com.example.airticket.enums.UserType;
import com.example.airticket.repository.FlightRepository;
import com.example.airticket.repository.FlightSegmentRepository;
import com.example.airticket.repository.MealOptionRepository;
import com.example.airticket.repository.MealReservationRepository;
import com.example.airticket.repository.TicketSaleRepository;
import com.example.airticket.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketServiceTest {
    private UserRepository userRepository;
    private FlightRepository flightRepository;
    private FlightSegmentRepository segmentRepository;
    private TicketSaleRepository ticketRepository;
    private MealOptionRepository mealRepository;
    private MealReservationRepository mealReservationRepository;
    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        flightRepository = mock(FlightRepository.class);
        segmentRepository = mock(FlightSegmentRepository.class);
        ticketRepository = mock(TicketSaleRepository.class);
        mealRepository = mock(MealOptionRepository.class);
        mealReservationRepository = mock(MealReservationRepository.class);
        ticketService = new TicketService(userRepository, flightRepository, segmentRepository, ticketRepository, mealRepository, mealReservationRepository);
    }

    @Test
    void createTicketLocksSegmentDeductsInventoryAndAppliesVipDiscount() {
        User vip = user(1, 1000, MemberLevel.VIP);
        Flight flight = flight(10);
        FlightSegment segment = segment(20, flight);
        CreateTicketRequest request = validCreateTicketRequest();
        request.mealId = 1;

        when(userRepository.findById(1)).thenReturn(Optional.of(vip));
        when(flightRepository.findById(10)).thenReturn(Optional.of(flight));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(segment));
        when(mealRepository.findById(1)).thenReturn(Optional.of(meal()));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale saved = ticketService.createTicket(request);

        assertThat(segment.economyRemainingSeats).isEqualTo(4);
        assertThat(saved.ticketStatus).isEqualTo(TicketStatus.PENDING_PAYMENT);
        assertThat(saved.priceAmount).isEqualByComparingTo("1000.00");
        assertThat(saved.paymentAmount).isEqualByComparingTo("900.00");
        assertThat(saved.expiredAt).isAfter(saved.bookedAt);
        verify(segmentRepository).findByIdForUpdate(20);
        verify(mealReservationRepository).save(any());
    }

    @Test
    void createTicketAppliesHalfPriceForSpecialOfferSegment() {
        User passenger = user(1, 0, MemberLevel.NORMAL);
        Flight flight = flight(10);
        FlightSegment segment = segment(20, flight);
        segment.isSpecialOffer = true;
        CreateTicketRequest request = validCreateTicketRequest();

        when(userRepository.findById(1)).thenReturn(Optional.of(passenger));
        when(flightRepository.findById(10)).thenReturn(Optional.of(flight));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(segment));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale saved = ticketService.createTicket(request);

        assertThat(saved.priceAmount).isEqualByComparingTo("500.00");
        assertThat(saved.paymentAmount).isEqualByComparingTo("500.00");
    }

    @Test
    void createTicketAppliesVipDiscountAfterSpecialOfferDiscount() {
        User vip = user(1, 1000, MemberLevel.VIP);
        Flight flight = flight(10);
        FlightSegment segment = segment(20, flight);
        segment.isSpecialOffer = true;
        CreateTicketRequest request = validCreateTicketRequest();

        when(userRepository.findById(1)).thenReturn(Optional.of(vip));
        when(flightRepository.findById(10)).thenReturn(Optional.of(flight));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(segment));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale saved = ticketService.createTicket(request);

        assertThat(saved.priceAmount).isEqualByComparingTo("500.00");
        assertThat(saved.paymentAmount).isEqualByComparingTo("450.00");
    }

    @Test
    void createTicketRejectsPassengerNameShorterThanTwoCharacters() {
        CreateTicketRequest request = validCreateTicketRequest();
        request.passengerName = "A";

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(com.example.airticket.exception.BusinessException.class)
                .satisfies(ex -> assertThat(((com.example.airticket.exception.BusinessException) ex).code).isEqualTo(40004));
    }

    @Test
    void createTicketRejectsInvalidPassengerIdNumberFormat() {
        CreateTicketRequest request = validCreateTicketRequest();
        request.passengerIdNumber = "123";

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(com.example.airticket.exception.BusinessException.class)
                .satisfies(ex -> assertThat(((com.example.airticket.exception.BusinessException) ex).code).isEqualTo(45002));
    }

    @Test
    void createTicketDoesNotUsePassengerIdNumberDigestToBypassValidation() {
        CreateTicketRequest request = validCreateTicketRequest();
        request.passengerIdNumber = "";
        request.passengerIdNumberDigest = "110101199001010011";

        assertThatThrownBy(() -> ticketService.createTicket(request))
                .isInstanceOf(com.example.airticket.exception.BusinessException.class)
                .satisfies(ex -> assertThat(((com.example.airticket.exception.BusinessException) ex).code).isEqualTo(40001));
    }

    @Test
    void payTicketAddsPointsAndUpgradesPassengerToVipAtThreshold() {
        User passenger = user(1, 900, MemberLevel.NORMAL);
        TicketSale ticket = new TicketSale();
        ticket.ticketId = 100;
        ticket.user = passenger;
        ticket.ticketStatus = TicketStatus.PENDING_PAYMENT;
        ticket.expiredAt = LocalDateTime.now().plusMinutes(5);
        ticket.paymentAmount = new BigDecimal("1000.00");
        PayTicketRequest request = new PayTicketRequest();
        request.ticketId = 100;

        when(ticketRepository.findByIdForUpdate(100)).thenReturn(Optional.of(ticket));
        when(userRepository.findByIdForUpdate(1)).thenReturn(Optional.of(passenger));

        TicketSale paid = ticketService.payTicket(request);

        assertThat(paid.ticketStatus).isEqualTo(TicketStatus.PAID);
        assertThat(paid.paidAt).isNotNull();
        assertThat(passenger.points).isEqualTo(1000);
        assertThat(passenger.memberLevel).isEqualTo(MemberLevel.VIP);
    }

    @Test
    void changeApplyCreatesPendingTicketLinkedToPreviousTicketAndLocksTargetSegment() {
        User passenger = user(1, 1000, MemberLevel.VIP);
        Flight oldFlight = flight(10);
        Flight newFlight = flight(11);
        FlightSegment oldSegment = segment(20, oldFlight);
        FlightSegment newSegment = segment(21, newFlight);
        newSegment.economyPrice = new BigDecimal("1200.00");
        TicketSale oldTicket = new TicketSale();
        oldTicket.ticketId = 100;
        oldTicket.user = passenger;
        oldTicket.flight = oldFlight;
        oldTicket.segment = oldSegment;
        oldTicket.cabinClass = CabinClass.ECONOMY;
        oldTicket.ticketStatus = TicketStatus.PAID;
        oldTicket.priceAmount = new BigDecimal("1000.00");
        oldTicket.paymentAmount = new BigDecimal("900.00");
        oldTicket.passengerName = "演示乘客";
        oldTicket.passengerIdNumberDigest = "digest";
        ChangeApplyRequest request = new ChangeApplyRequest();
        request.ticketId = 100;
        request.targetFlightId = 11;
        request.targetSegmentId = 21;

        when(ticketRepository.findByIdForUpdate(100)).thenReturn(Optional.of(oldTicket));
        when(flightRepository.findById(11)).thenReturn(Optional.of(newFlight));
        when(segmentRepository.findByIdForUpdate(21)).thenReturn(Optional.of(newSegment));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(oldSegment));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale changeTicket = ticketService.applyChange(request);

        assertThat(newSegment.economyRemainingSeats).isEqualTo(4);
        assertThat(changeTicket.originalTicket).isSameAs(oldTicket);
        assertThat(changeTicket.ticketStatus).isEqualTo(TicketStatus.PENDING_PAYMENT);
        assertThat(changeTicket.paymentAmount).isEqualByComparingTo("180.00");
        verify(segmentRepository).findByIdForUpdate(21);
    }

    @Test
    void changeApplyUsesRequestedCabinClassInventoryAndPrice() {
        User passenger = user(1, 0, MemberLevel.NORMAL);
        Flight oldFlight = flight(10);
        Flight newFlight = flight(11);
        FlightSegment oldSegment = segment(20, oldFlight);
        FlightSegment newSegment = segment(21, newFlight);
        newSegment.firstClassPrice = new BigDecimal("2200.00");
        TicketSale oldTicket = paidTicket(passenger, oldFlight, oldSegment, CabinClass.ECONOMY, "1000.00", "1000.00");
        ChangeApplyRequest request = validChangeRequest();
        request.cabinClass = CabinClass.FIRST_CLASS.name();

        when(ticketRepository.findByIdForUpdate(100)).thenReturn(Optional.of(oldTicket));
        when(flightRepository.findById(11)).thenReturn(Optional.of(newFlight));
        when(segmentRepository.findByIdForUpdate(21)).thenReturn(Optional.of(newSegment));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(oldSegment));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale changeTicket = ticketService.applyChange(request);

        assertThat(newSegment.firstClassRemainingSeats).isEqualTo(1);
        assertThat(newSegment.economyRemainingSeats).isEqualTo(5);
        assertThat(changeTicket.cabinClass).isEqualTo(CabinClass.FIRST_CLASS);
        assertThat(changeTicket.priceAmount).isEqualByComparingTo("2200.00");
        assertThat(changeTicket.paymentAmount).isEqualByComparingTo("1200.00");
    }

    @Test
    void changeApplySavesMealReservationWhenMealIsSelected() {
        User passenger = user(1, 0, MemberLevel.NORMAL);
        Flight oldFlight = flight(10);
        Flight newFlight = flight(11);
        FlightSegment oldSegment = segment(20, oldFlight);
        FlightSegment newSegment = segment(21, newFlight);
        TicketSale oldTicket = paidTicket(passenger, oldFlight, oldSegment, CabinClass.ECONOMY, "1000.00", "1000.00");
        ChangeApplyRequest request = validChangeRequest();
        request.mealId = 1;

        when(ticketRepository.findByIdForUpdate(100)).thenReturn(Optional.of(oldTicket));
        when(flightRepository.findById(11)).thenReturn(Optional.of(newFlight));
        when(segmentRepository.findByIdForUpdate(21)).thenReturn(Optional.of(newSegment));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(oldSegment));
        when(mealRepository.findById(1)).thenReturn(Optional.of(meal()));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale changeTicket = ticketService.applyChange(request);

        verify(mealReservationRepository).save(any(MealReservation.class));
        assertThat(changeTicket.originalTicket).isSameAs(oldTicket);
    }

    @Test
    void changeApplyAutoCompletesWhenTargetFlightIsCheaper() {
        User passenger = user(1, 0, MemberLevel.NORMAL);
        Flight oldFlight = flight(10);
        Flight newFlight = flight(11);
        FlightSegment oldSegment = segment(20, oldFlight);
        FlightSegment newSegment = segment(21, newFlight);
        newSegment.economyPrice = new BigDecimal("600.00");
        TicketSale oldTicket = paidTicket(passenger, oldFlight, oldSegment, CabinClass.ECONOMY, "1000.00", "1000.00");
        ChangeApplyRequest request = validChangeRequest();

        when(ticketRepository.findByIdForUpdate(100)).thenReturn(Optional.of(oldTicket));
        when(flightRepository.findById(11)).thenReturn(Optional.of(newFlight));
        when(segmentRepository.findByIdForUpdate(21)).thenReturn(Optional.of(newSegment));
        when(segmentRepository.findByIdForUpdate(20)).thenReturn(Optional.of(oldSegment));
        when(ticketRepository.save(any(TicketSale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketSale changeTicket = ticketService.applyChange(request);

        assertThat(changeTicket.paymentAmount).isEqualByComparingTo("0.00");
        assertThat(changeTicket.ticketStatus).isEqualTo(TicketStatus.PAID);
        assertThat(oldTicket.ticketStatus).isEqualTo(TicketStatus.CHANGE_SUCCESS);
        assertThat(oldSegment.economyRemainingSeats).isEqualTo(6);
    }

    private User user(int id, int points, MemberLevel level) {
        User user = new User();
        user.userId = id;
        user.loginAccount = "passenger" + id;
        user.userName = "演示乘客" + id;
        user.passwordHash = "hash";
        user.idNumberDigest = "id" + id;
        user.userType = UserType.PASSENGER;
        user.points = points;
        user.memberLevel = level;
        return user;
    }

    private Flight flight(int id) {
        Flight flight = new Flight();
        flight.flightId = id;
        flight.flightNumber = "MU" + id;
        flight.flightDate = LocalDate.of(2026, 7, 1);
        flight.flightStatus = FlightStatus.NORMAL;
        return flight;
    }

    private FlightSegment segment(int id, Flight flight) {
        FlightSegment segment = new FlightSegment();
        segment.segmentId = id;
        segment.flight = flight;
        segment.originStopNo = 1;
        segment.destinationStopNo = 2;
        segment.originAirportCode = "PEK";
        segment.destinationAirportCode = "PVG";
        segment.plannedDepartureTime = LocalDateTime.of(2026, 7, 1, 8, 0);
        segment.plannedArrivalTime = LocalDateTime.of(2026, 7, 1, 10, 0);
        segment.firstClassRemainingSeats = 2;
        segment.economyRemainingSeats = 5;
        segment.firstClassPrice = new BigDecimal("2000.00");
        segment.economyPrice = new BigDecimal("1000.00");
        return segment;
    }

    private MealOption meal() {
        MealOption meal = new MealOption();
        meal.mealId = 1;
        meal.mealName = "普通餐";
        meal.mealType = "NORMAL";
        meal.isAvailable = true;
        return meal;
    }

    private ChangeApplyRequest validChangeRequest() {
        ChangeApplyRequest request = new ChangeApplyRequest();
        request.ticketId = 100;
        request.targetFlightId = 11;
        request.targetSegmentId = 21;
        return request;
    }

    private TicketSale paidTicket(User passenger, Flight flight, FlightSegment segment, CabinClass cabinClass, String priceAmount, String paymentAmount) {
        TicketSale ticket = new TicketSale();
        ticket.ticketId = 100;
        ticket.user = passenger;
        ticket.flight = flight;
        ticket.segment = segment;
        ticket.cabinClass = cabinClass;
        ticket.ticketStatus = TicketStatus.PAID;
        ticket.priceAmount = new BigDecimal(priceAmount);
        ticket.paymentAmount = new BigDecimal(paymentAmount);
        ticket.passengerName = "婕旂ず涔樺";
        ticket.passengerIdNumberDigest = "digest";
        return ticket;
    }

    private CreateTicketRequest validCreateTicketRequest() {
        CreateTicketRequest request = new CreateTicketRequest();
        request.userId = 1;
        request.flightId = 10;
        request.segmentId = 20;
        request.cabinClass = CabinClass.ECONOMY.name();
        request.passengerName = "演示乘客";
        request.passengerIdNumber = "110101199001010011";
        return request;
    }
}
