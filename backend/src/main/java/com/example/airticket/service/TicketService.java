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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketService {
    private static final int PAYMENT_WINDOW_MINUTES = 15;
    private static final int TICKET_REWARD_POINTS = 100;
    private static final int VIP_THRESHOLD = 1000;
    private static final BigDecimal VIP_DISCOUNT = new BigDecimal("0.90");

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
        User user = userRepository.findById(request.userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        Flight flight = flightRepository.findById(request.flightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
        ensureFlightSellable(flight);
        FlightSegment segment = segmentRepository.findByIdForUpdate(request.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
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
        ticket.passengerName = request.passengerName;
        ticket.passengerIdNumberDigest = SecurityUtil.sha256Digest(firstNonBlank(request.passengerIdNumber, request.passengerIdNumberDigest));
        ticket.priceAmount = price;
        ticket.paymentAmount = effectivePayment(price, user.memberLevel);
        ticket.bookedAt = LocalDateTime.now();
        ticket.expiredAt = ticket.bookedAt.plusMinutes(PAYMENT_WINDOW_MINUTES);
        TicketSale saved = ticketRepository.save(ticket);
        saveMealIfSelected(request.mealId, saved);
        return saved;
    }

    @Transactional
    public TicketSale payTicket(PayTicketRequest request) {
        TicketSale ticket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "订单不存在"));
        ensurePending(ticket);
        if (ticket.expiredAt != null && LocalDateTime.now().isAfter(ticket.expiredAt)) {
            expireLockedTicket(ticket);
            throw new BusinessException(42005, "订单已超时，请重新下单");
        }
        ticket.ticketStatus = TicketStatus.PAID;
        ticket.paidAt = LocalDateTime.now();
        ticket.issuedAt = ticket.paidAt;
        User user = userRepository.findByIdForUpdate(ticket.user.userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        user.points = safePoints(user) + TICKET_REWARD_POINTS;
        refreshMemberLevel(user);
        return ticket;
    }

    @Transactional
    public TicketSale refundTicket(RefundTicketRequest request) {
        TicketSale ticket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "订单不存在"));
        if (ticket.ticketStatus != TicketStatus.PAID) {
            throw new BusinessException(42019, "原票状态不允许退票");
        }
        FlightSegment segment = segmentRepository.findByIdForUpdate(ticket.segment.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
        restoreInventory(segment, ticket.cabinClass);
        ticket.ticketStatus = TicketStatus.REFUND_SUCCESS;
        ticket.refundedAt = LocalDateTime.now();
        ticket.remark = request.remark;
        User user = userRepository.findByIdForUpdate(ticket.user.userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
        user.points = Math.max(0, safePoints(user) - TICKET_REWARD_POINTS);
        refreshMemberLevel(user);
        return ticket;
    }

    @Transactional
    public TicketSale applyChange(ChangeApplyRequest request) {
        TicketSale oldTicket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "订单不存在"));
        if (oldTicket.ticketStatus != TicketStatus.PAID) {
            throw new BusinessException(42018, "原票状态不允许改签");
        }
        Integer targetFlightId = request.targetFlightId != null ? request.targetFlightId : request.flightId;
        Integer targetSegmentId = request.targetSegmentId != null ? request.targetSegmentId : request.segmentId;
        Flight flight = flightRepository.findById(targetFlightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
        ensureFlightSellable(flight);
        FlightSegment segment = segmentRepository.findByIdForUpdate(targetSegmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
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
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            completeChange(oldTicket, saved);
        }
        return saved;
    }

    @Transactional
    public TicketSale payChange(ChangePayRequest request) {
        TicketSale newTicket = ticketRepository.findByIdForUpdate(request.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "订单不存在"));
        ensurePending(newTicket);
        if (newTicket.originalTicket == null) {
            throw new BusinessException(42014, "改签订单不存在");
        }
        if (newTicket.expiredAt != null && LocalDateTime.now().isAfter(newTicket.expiredAt)) {
            expireLockedTicket(newTicket);
            throw new BusinessException(42017, "改签订单已过期");
        }
        TicketSale oldTicket = ticketRepository.findByIdForUpdate(newTicket.originalTicket.ticketId)
                .orElseThrow(() -> new BusinessException(40407, "原订单不存在"));
        if (oldTicket.ticketStatus != TicketStatus.PAID) {
            throw new BusinessException(42018, "原票状态不允许改签");
        }
        completeChange(oldTicket, newTicket);
        return newTicket;
    }

    public TicketSale detail(Integer ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException(40407, "订单不存在"));
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
                count++;
            }
        }
        return count;
    }

    private void completeChange(TicketSale oldTicket, TicketSale newTicket) {
        oldTicket.ticketStatus = TicketStatus.CHANGE_SUCCESS;
        oldTicket.changedAt = LocalDateTime.now();
        newTicket.ticketStatus = TicketStatus.PAID;
        newTicket.paidAt = oldTicket.changedAt;
        newTicket.issuedAt = oldTicket.changedAt;
    }

    private void expireLockedTicket(TicketSale ticket) {
        FlightSegment segment = segmentRepository.findByIdForUpdate(ticket.segment.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
        restoreInventory(segment, ticket.cabinClass);
        ticket.ticketStatus = TicketStatus.EXPIRED;
    }

    private void saveMealIfSelected(Integer mealId, TicketSale ticket) {
        if (mealId == null) {
            return;
        }
        MealOption meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(40408, "餐食不存在"));
        if (Boolean.FALSE.equals(meal.isAvailable)) {
            throw new BusinessException(44001, "餐食已停用");
        }
        MealReservation reservation = new MealReservation();
        reservation.ticket = ticket;
        reservation.meal = meal;
        reservation.quantity = 1;
        mealReservationRepository.save(reservation);
    }

    private void ensureFlightSellable(Flight flight) {
        if (flight.flightStatus == FlightStatus.DISABLED) {
            throw new BusinessException(41001, "航班已停用，无法继续操作");
        }
        if (flight.flightStatus == FlightStatus.CANCELLED) {
            throw new BusinessException(41002, "航班已取消，不允许继续售卖");
        }
        if (flight.flightStatus == FlightStatus.COMPLETED) {
            throw new BusinessException(41003, "航班已完成，不允许继续售卖");
        }
    }

    private void ensureSegmentBelongsToFlight(FlightSegment segment, Flight flight) {
        if (segment.flight == null || !segment.flight.flightId.equals(flight.flightId)) {
            throw new BusinessException(41004, "航段不存在或不属于该航班");
        }
    }

    private void ensurePending(TicketSale ticket) {
        if (ticket.ticketStatus != TicketStatus.PENDING_PAYMENT) {
            throw new BusinessException(42003, "订单状态不允许当前操作");
        }
    }

    private void deductInventory(FlightSegment segment, CabinClass cabinClass) {
        if (cabinClass == CabinClass.ECONOMY) {
            if (segment.economyRemainingSeats == null || segment.economyRemainingSeats <= 0) {
                throw new BusinessException(42001, "当前航段余票不足");
            }
            segment.economyRemainingSeats -= 1;
        } else {
            if (segment.firstClassRemainingSeats == null || segment.firstClassRemainingSeats <= 0) {
                throw new BusinessException(42001, "当前航段余票不足");
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
            throw new BusinessException(41009, "舱位类型非法");
        }
    }

    private BigDecimal priceFor(FlightSegment segment, CabinClass cabinClass) {
        return cabinClass == CabinClass.ECONOMY ? segment.economyPrice : segment.firstClassPrice;
    }

    private BigDecimal effectivePayment(BigDecimal price, MemberLevel memberLevel) {
        BigDecimal value = memberLevel == MemberLevel.VIP ? price.multiply(VIP_DISCOUNT) : price;
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private int safePoints(User user) {
        return user.points == null ? 0 : user.points;
    }

    private void refreshMemberLevel(User user) {
        user.memberLevel = safePoints(user) >= VIP_THRESHOLD ? MemberLevel.VIP : MemberLevel.NORMAL;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second;
    }
}
