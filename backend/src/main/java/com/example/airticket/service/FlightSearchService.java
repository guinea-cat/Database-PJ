package com.example.airticket.service;

import com.example.airticket.entity.Airport;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.enums.FlightStatus;
import com.example.airticket.repository.AirportRepository;
import com.example.airticket.repository.FlightSegmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FlightSearchService {
    private final AirportRepository airportRepository;
    private final FlightSegmentRepository segmentRepository;

    public FlightSearchService(AirportRepository airportRepository, FlightSegmentRepository segmentRepository) {
        this.airportRepository = airportRepository;
        this.segmentRepository = segmentRepository;
    }

    public List<FlightSegment> search(Integer departureCityId,
                                      Integer arrivalCityId,
                                      String departureAirportCode,
                                      String arrivalAirportCode,
                                      LocalDate flightDateStart,
                                      LocalDate flightDateEnd) {
        LocalDate start = flightDateStart == null ? LocalDate.of(2026, 6, 28) : flightDateStart;
        LocalDate end = flightDateEnd == null ? LocalDate.of(2026, 7, 4) : flightDateEnd;
        List<String> origins = airportCodes(departureCityId, departureAirportCode);
        List<String> destinations = airportCodes(arrivalCityId, arrivalAirportCode);
        return segmentRepository.searchByAirports(origins, destinations, start, end)
                .stream()
                .filter(segment -> segment.flight.flightStatus == FlightStatus.NORMAL || segment.flight.flightStatus == FlightStatus.DELAYED)
                .collect(Collectors.toList());
    }

    private List<String> airportCodes(Integer cityId, String airportCode) {
        if (airportCode != null && !airportCode.trim().isEmpty()) {
            return List.of(airportCode);
        }
        return airportRepository.findByCityCityId(cityId)
                .stream()
                .map(airport -> airport.airportCode)
                .collect(Collectors.toList());
    }
}
