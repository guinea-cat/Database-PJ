package com.example.airticket.service;

import com.example.airticket.dto.request.ChangeApplyRequest;
import com.example.airticket.dto.request.ChangePayRequest;
import com.example.airticket.dto.request.CreateTicketRequest;
import com.example.airticket.dto.request.PayTicketRequest;
import com.example.airticket.dto.request.RefundTicketRequest;
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
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.FlightRepository;
import com.example.airticket.repository.FlightSegmentRepository;
import com.example.airticket.repository.MealOptionRepository;
import com.example.airticket.repository.MealReservationRepository;
import com.example.airticket.repository.TicketSaleRepository;
import com.example.airticket.repository.UserRepository;
import com.example.airticket.util.OrderNoGenerator;
import com.example.airticket.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class TicketService {
    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private static final int PAYMENT_WINDOW_MINUTES = 15;
    private static final int TICKET_REWARD_POINTS = 100;
    private static final int VIP_THRESHOLD = 1000;
    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("0.90");
    private static final BigDecimal SPECIAL_OFFER_DISCOUNT = new BigDecimal("0.50");
    private static final int PASSENGER_NAME_MIN_LENGTH = 2;
    private static final int PASSENGER_NAME_MAX_LENGTH = 50;
    private static final Pattern PASSENGER_ID_NUMBER_PATTERN = Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$");

    private final UserRepository userRepository;
    private final FlightRepository flightRepository;
    private final FlightSegmentRepository segmentRepository;
    private final TicketSaleRepository ticketRepository;
    private final MealOptionRepository mealRepository;
    private final MealReservationRepository mealReservationRepository;

    public TicketService(UserRepository userRepository,
                         FlightRepository flightRepository,
                         FlightSegmentRepository segmentRepository,
                         TicketSaleRepository ticketRepository,
                         MealOptionRepository mealRepository,
                         MealReservationRepository mealReservationRepository) {
        this.userRepository = userRepository;
        this.flightRepository = flightRepository;
        this.segmentRepository = segmentRepository;
        this.ticketRepository = ticketRepository;
        this.mealRepository = mealRepository;
        this.mealReservationRepository = mealReservationRepository;
    }

    @Transactional
    public TicketSale createTicket(CreateTicketRequest request) {
        String passengerName = normalizePassengerName(request.passengerName);
        String passengerIdNumber = normalizePassengerIdNumber(request.passengerIdNumber);

        User user = userRepository.findById(request.userId)
                .orElseThrow(() -> new BusinessException(40401, "user not found"));
        Flight flight = flightRepository.findById(request.flightId)
                .orElseThrow(() -> new BusinessException(40405, "flight not found"));
        ensureFlightSellable(flight);
        FlightSegment segment = segmentRepository.findByIdForUpdate(request.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "segment not found"));
        ensureSegmentBelongsToFlight(segment, flight);
        CabinClass cabinClass = parseCabinClass(request.cabinClass);
        BigDecimal price = priceFor(segment, cabinClass);
        deductInventory(segment, cabinClass);

        TicketSale ticket = new TicketSale();
        ticket.orderNo = OrderNoGenerator.next();
        ticket.user = user;
        ticket.flight = flight;
        ticket.segment = segment;
        ticket.cabinClass = cabinClass;
        ticket.ticketStatus = TicketStatus.PENDING_PAYMENT;
        ticket.passengerName = passengerName;
        ticket.passengerIdNumberDigest = SecurityUtil.sha256Digest(passengerIdNumber);
        ticket.priceAmount = price;
        ticket.paymentAmount = effectivePayment(price, user.memberLevel);
        ticket.bookedAt = LocalDateTime.now();
        ticket.expiredAt = ticket.bookedAt.plusMinutes(PAYMENT_WINDOW_MINUTES);
        TicketSale saved = ticketRepository.save(ticket);
        saveMealIfSelected(request.mealId, saved);
        log.info("ticket.create ticketId={} orderNo={} userId={} segmentId={} cabinClass={} specialOffer={} priceAmount={} paymentAmount={}",
                saved.ticketId, saved.orderNo, user.userId, segment.segmentId, cabinClass, segment.isSpecialOffer, saved.priceAmount, saved.paymentAmount);
        return saved;
    }

    @Transactional
    public TicketSale payTicket(PayTicketRequest request) {
        TicketSale ticket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "ticket not found"));
        ensurePending(ticket);
        if (ticket.expiredAt != null && LocalDateTime.now().isAfter(ticket.expiredAt)) {
            expireLockedTicket(ticket);
            throw new BusinessException(42005, "order expired");
        }
        ticket.ticketStatus = TicketStatus.PAID;
        ticket.paidAt = LocalDateTime.now();
        ticket.issuedAt = ticket.paidAt;
        User user = userRepository.findByIdForUpdate(ticket.user.userId)
                .orElseThrow(() -> new BusinessException(40401, "user not found"));
        user.points = safePoints(user) + TICKET_REWARD_POINTS;
        refreshMemberLevel(user);
        log.info("ticket.pay ticketId={} orderNo={} userId={} points={} memberLevel={}",
                ticket.ticketId, ticket.orderNo, user.userId, user.points, user.memberLevel);
        return ticket;
    }

    @Transactional
    public TicketSale refundTicket(RefundTicketRequest request) {
        TicketSale ticket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "ticket not found"));
        if (ticket.ticketStatus != TicketStatus.PAID) {
            throw new BusinessException(42019, "ticket status does not allow refund");
        }
        FlightSegment segment = segmentRepository.findByIdForUpdate(ticket.segment.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "segment not found"));
        restoreInventory(segment, ticket.cabinClass);
        ticket.ticketStatus = TicketStatus.REFUND_SUCCESS;
        ticket.refundedAt = LocalDateTime.now();
        ticket.remark = request.remark;
        User user = userRepository.findByIdForUpdate(ticket.user.userId)
                .orElseThrow(() -> new BusinessException(40401, "user not found"));
        user.points = Math.max(0, safePoints(user) - TICKET_REWARD_POINTS);
        refreshMemberLevel(user);
        log.info("ticket.refund ticketId={} orderNo={} userId={} segmentId={} points={}",
                ticket.ticketId, ticket.orderNo, user.userId, segment.segmentId, user.points);
        return ticket;
    }

    @Transactional
    public TicketSale applyChange(ChangeApplyRequest request) {
        TicketSale oldTicket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "ticket not found"));
        if (oldTicket.ticketStatus != TicketStatus.PAID) {
            throw new BusinessException(42018, "ticket status does not allow change");
        }
        Integer targetFlightId = request.targetFlightId != null ? request.targetFlightId : request.flightId;
        Integer targetSegmentId = request.targetSegmentId != null ? request.targetSegmentId : request.segmentId;
        Flight flight = flightRepository.findById(targetFlightId)
                .orElseThrow(() -> new BusinessException(40405, "flight not found"));
        ensureFlightSellable(flight);
        FlightSegment segment = segmentRepository.findByIdForUpdate(targetSegmentId)
                .orElseThrow(() -> new BusinessException(40406, "segment not found"));
        ensureSegmentBelongsToFlight(segment, flight);
        CabinClass cabinClass = request.cabinClass == null ? oldTicket.cabinClass : parseCabinClass(request.cabinClass);
        BigDecimal newPrice = priceFor(segment, cabinClass);
        BigDecimal newEffectivePayment = effectivePayment(newPrice, oldTicket.user.memberLevel);
        BigDecimal difference = newEffectivePayment.subtract(oldTicket.paymentAmount == null ? BigDecimal.ZERO : oldTicket.paymentAmount);
        if (difference.signum() < 0) {
            difference = BigDecimal.ZERO;
        }
        deductInventory(segment, cabinClass);

        TicketSale newTicket = new TicketSale();
        newTicket.orderNo = OrderNoGenerator.next();
        newTicket.user = oldTicket.user;
        newTicket.flight = flight;
        newTicket.segment = segment;
        newTicket.cabinClass = cabinClass;
        newTicket.ticketStatus = TicketStatus.PENDING_PAYMENT;
        newTicket.passengerName = oldTicket.passengerName;
        newTicket.passengerIdNumberDigest = oldTicket.passengerIdNumberDigest;
        newTicket.priceAmount = newPrice;
        newTicket.paymentAmount = difference.setScale(2, RoundingMode.HALF_UP);
        newTicket.originalTicket = oldTicket;
        newTicket.changeReason = request.changeReason;
        newTicket.bookedAt = LocalDateTime.now();
        newTicket.expiredAt = newTicket.bookedAt.plusMinutes(PAYMENT_WINDOW_MINUTES);
        TicketSale saved = ticketRepository.save(newTicket);
        saveMealIfSelected(request.mealId, saved);
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            completeChange(oldTicket, saved);
        }
        log.info("ticket.changeApply oldTicketId={} newTicketId={} targetSegmentId={} difference={} status={}",
                oldTicket.ticketId, saved.ticketId, segment.segmentId, saved.paymentAmount, saved.ticketStatus);
        return saved;
    }

    @Transactional
    public TicketSale payChange(ChangePayRequest request) {
        TicketSale newTicket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "ticket not found"));
        ensurePending(newTicket);
        if (newTicket.originalTicket == null) {
            throw new BusinessException(42014, "change ticket missing original ticket");
        }
        if (newTicket.expiredAt != null && LocalDateTime.now().isAfter(newTicket.expiredAt)) {
            expireLockedTicket(newTicket);
            throw new BusinessException(42017, "change order expired");
        }
        TicketSale oldTicket = ticketRepository.findByIdForUpdate(newTicket.originalTicket.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "original ticket not found"));
        if (oldTicket.ticketStatus != TicketStatus.PAID) {
            throw new BusinessException(42018, "ticket status does not allow change");
        }
        completeChange(oldTicket, newTicket);
        log.info("ticket.changePay oldTicketId={} newTicketId={} orderNo={}",
                oldTicket.ticketId, newTicket.ticketId, newTicket.orderNo);
        return newTicket;
    }

    public TicketSale detail(Integer ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(40407, "ticket not found"));
    }

    public List<TicketSale> listByUser(Integer userId) {
        return ticketRepository.findByUserUserId(userId);
    }

    @Transactional
    public int expirePendingOrders() {
        List<TicketSale> tickets = ticketRepository.findByTicketStatusAndExpiredAtBefore(TicketStatus.PENDING_PAYMENT, LocalDateTime.now());
        int count = 0;
        for (TicketSale ticket : tickets) {
            TicketSale locked = ticketRepository.findByIdForUpdate(ticket.ticketId).orElse(null);
            if (locked != null && locked.ticketStatus == TicketStatus.PENDING_PAYMENT) {
                expireLockedTicket(locked);
                log.info("ticket.expire ticketId={} orderNo={} segmentId={}", locked.ticketId, locked.orderNo, locked.segment.segmentId);
                count++;
            }
        }
        return count;
    }

    private void completeChange(TicketSale oldTicket, TicketSale newTicket) {
        FlightSegment oldSegment = segmentRepository.findByIdForUpdate(oldTicket.segment.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "segment not found"));
        restoreInventory(oldSegment, oldTicket.cabinClass);
        oldTicket.ticketStatus = TicketStatus.CHANGE_SUCCESS;
        oldTicket.changedAt = LocalDateTime.now();
        newTicket.ticketStatus = TicketStatus.PAID;
        newTicket.paidAt = oldTicket.changedAt;
        newTicket.issuedAt = oldTicket.changedAt;
    }

    private void expireLockedTicket(TicketSale ticket) {
        FlightSegment segment = segmentRepository.findByIdForUpdate(ticket.segment.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "segment not found"));
        restoreInventory(segment, ticket.cabinClass);
        ticket.ticketStatus = TicketStatus.EXPIRED;
    }

    private void saveMealIfSelected(Integer mealId, TicketSale ticket) {
        if (mealId == null) {
            return;
        }
        MealOption meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(40408, "meal not found"));
        if (Boolean.FALSE.equals(meal.isAvailable)) {
            throw new BusinessException(44001, "meal unavailable");
        }
        MealReservation reservation = new MealReservation();
        reservation.ticket = ticket;
        reservation.meal = meal;
        reservation.quantity = 1;
        mealReservationRepository.save(reservation);
    }

    private void ensureFlightSellable(Flight flight) {
        if (flight.flightStatus == FlightStatus.DISABLED) {
            throw new BusinessException(41001, "flight disabled");
        }
        if (flight.flightStatus == FlightStatus.CANCELLED) {
            throw new BusinessException(41002, "flight cancelled");
        }
        if (flight.flightStatus == FlightStatus.COMPLETED) {
            throw new BusinessException(41003, "flight completed");
        }
    }

    private void ensureSegmentBelongsToFlight(FlightSegment segment, Flight flight) {
        if (segment.flight == null || !segment.flight.flightId.equals(flight.flightId)) {
            throw new BusinessException(41004, "segment does not belong to flight");
        }
    }

    private void ensurePending(TicketSale ticket) {
        if (ticket.ticketStatus != TicketStatus.PENDING_PAYMENT) {
            throw new BusinessException(42003, "ticket status does not allow current operation");
        }
    }

    private void deductInventory(FlightSegment segment, CabinClass cabinClass) {
        if (cabinClass == CabinClass.ECONOMY) {
            if (segment.economyRemainingSeats == null || segment.economyRemainingSeats <= 0) {
                throw new BusinessException(42001, "not enough seats");
            }
            segment.economyRemainingSeats -= 1;
        } else {
            if (segment.firstClassRemainingSeats == null || segment.firstClassRemainingSeats <= 0) {
                throw new BusinessException(42001, "not enough seats");
            }
            segment.firstClassRemainingSeats -= 1;
        }
    }

    private void restoreInventory(FlightSegment segment, CabinClass cabinClass) {
        if (cabinClass == CabinClass.ECONOMY) {
            segment.economyRemainingSeats = (segment.economyRemainingSeats == null ? 0 : segment.economyRemainingSeats) + 1;
        } else {
            segment.firstClassRemainingSeats = (segment.firstClassRemainingSeats == null ? 0 : segment.firstClassRemainingSeats) + 1;
        }
    }

    private CabinClass parseCabinClass(String value) {
        try {
            return CabinClass.valueOf(value);
        } catch (Exception ex) {
            throw new BusinessException(41009, "invalid cabin class");
        }
    }

    private BigDecimal priceFor(FlightSegment segment, CabinClass cabinClass) {
        BigDecimal originalPrice = cabinClass == CabinClass.ECONOMY ? segment.economyPrice : segment.firstClassPrice;
        BigDecimal price = Boolean.TRUE.equals(segment.isSpecialOffer) ? originalPrice.multiply(SPECIAL_OFFER_DISCOUNT) : originalPrice;
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal effectivePayment(BigDecimal price, MemberLevel memberLevel) {
        BigDecimal value = memberLevel == MemberLevel.VIP ? price.multiply(VIP_DISCOUNT) : price;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizePassengerName(String passengerName) {
        String normalized = requireTrimmedText(passengerName, 40001, "passenger name required");
        if (normalized.length() < PASSENGER_NAME_MIN_LENGTH) {
            throw new BusinessException(40004, "passenger name too short");
        }
        if (normalized.length() > PASSENGER_NAME_MAX_LENGTH) {
            throw new BusinessException(40003, "passenger name too long");
        }
        return normalized;
    }

    private String normalizePassengerIdNumber(String passengerIdNumber) {
        String normalized = requireTrimmedText(passengerIdNumber, 40001, "passenger id required");
        if (!PASSENGER_ID_NUMBER_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(45002, "invalid passenger id number");
        }
        return normalized;
    }

    private String requireTrimmedText(String value, int code, String message) {
        if (value == null) {
            throw new BusinessException(code, message);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(code, message);
        }
        return normalized;
    }

    private int safePoints(User user) {
        return user.points == null ? 0 : user.points;
    }

    private void refreshMemberLevel(User user) {
        user.memberLevel = safePoints(user) >= VIP_THRESHOLD ? MemberLevel.VIP : MemberLevel.NORMAL;
    }
}
