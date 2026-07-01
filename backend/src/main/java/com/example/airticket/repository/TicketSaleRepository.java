package com.example.airticket.repository;

import com.example.airticket.entity.TicketSale;
import com.example.airticket.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TicketSaleRepository extends JpaRepository<TicketSale, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TicketSale t where t.ticketId = ?1")
    Optional<TicketSale> findByIdForUpdate(Integer ticketId);

    List<TicketSale> findByUserUserId(Integer userId);

    List<TicketSale> findByOriginalTicketTicketId(Integer ticketId);

    List<TicketSale> findByTicketStatusAndExpiredAtBefore(TicketStatus ticketStatus, LocalDateTime expiredAt);

    boolean existsByUserUserIdAndFlightFlightIdAndTicketStatusIn(Integer userId, Integer flightId, Collection<TicketStatus> ticketStatuses);

    long countByFlightFlightId(Integer flightId);
}
