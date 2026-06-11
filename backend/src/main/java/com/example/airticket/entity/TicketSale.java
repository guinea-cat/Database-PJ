package com.example.airticket.entity;

import com.example.airticket.enums.CabinClass;
import com.example.airticket.enums.TicketStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "TicketSale")
public class TicketSale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TicketId")
    public Integer ticketId;

    @Column(name = "OrderNo", nullable = false, unique = true)
    public String orderNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UserId", nullable = false)
    public User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "FlightId", nullable = false)
    public Flight flight;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SegmentId", nullable = false)
    public FlightSegment segment;

    @Enumerated(EnumType.STRING)
    @Column(name = "CabinClass", nullable = false)
    public CabinClass cabinClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "TicketStatus", nullable = false)
    public TicketStatus ticketStatus;

    @Column(name = "PassengerName", nullable = false)
    public String passengerName;

    @Column(name = "PassengerIdNumberDigest", nullable = false)
    public String passengerIdNumberDigest;

    @Column(name = "PriceAmount", nullable = false)
    public BigDecimal priceAmount;

    @Column(name = "PaymentAmount", nullable = false)
    public BigDecimal paymentAmount;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "OriginalTicketId")
    public TicketSale originalTicket;

    @Column(name = "ChangeReason")
    public String changeReason;

    @Column(name = "BookedAt", nullable = false)
    public LocalDateTime bookedAt;

    @Column(name = "PaidAt")
    public LocalDateTime paidAt;

    @Column(name = "IssuedAt")
    public LocalDateTime issuedAt;

    @Column(name = "ChangedAt")
    public LocalDateTime changedAt;

    @Column(name = "RefundedAt")
    public LocalDateTime refundedAt;

    @Column(name = "ExpiredAt")
    public LocalDateTime expiredAt;

    @Column(name = "Remark")
    public String remark;
}
