package com.example.airticket.repository;

import com.example.airticket.entity.FlightSegment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FlightSegmentRepository extends JpaRepository<FlightSegment, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from FlightSegment s where s.segmentId = ?1")
    Optional<FlightSegment> findByIdForUpdate(Integer segmentId);

    List<FlightSegment> findByFlightFlightId(Integer flightId);

    Optional<FlightSegment> findByFlightFlightIdAndOriginStopNoAndDestinationStopNo(Integer flightId, Integer originStopNo, Integer destinationStopNo);

    @Query("select s from FlightSegment s where s.originAirportCode in ?1 and s.destinationAirportCode in ?2 and s.flight.flightDate between ?3 and ?4")
    List<FlightSegment> searchByAirports(Collection<String> originCodes, Collection<String> destinationCodes, LocalDate start, LocalDate end);
}
