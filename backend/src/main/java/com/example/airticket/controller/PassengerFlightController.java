package com.example.airticket.controller;

import com.example.airticket.common.ApiResponse;
import com.example.airticket.dto.response.FlightSearchItemResponse;
import com.example.airticket.dto.response.TicketResponse;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.FlightSegmentRepository;
import com.example.airticket.service.FlightSearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/flight")
public class PassengerFlightController {
    private final FlightSearchService flightSearchService;
    private final FlightSegmentRepository segmentRepository;

    public PassengerFlightController(FlightSearchService flightSearchService, FlightSegmentRepository segmentRepository) {
        this.flightSearchService = flightSearchService;
        this.segmentRepository = segmentRepository;
    }

    @GetMapping("/search")
    public ApiResponse<List<FlightSearchItemResponse>> search(
            @RequestParam(required = false) Integer departureCityId,
            @RequestParam(required = false) Integer arrivalCityId,
            @RequestParam(required = false) String departureAirportCode,
            @RequestParam(required = false) String arrivalAirportCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate flightDateEnd) {
        LocalDate start = flightDate != null ? flightDate : flightDateStart;
        LocalDate end = flightDate != null ? flightDate : flightDateEnd;
        List<FlightSearchItemResponse> data = flightSearchService
                .search(departureCityId, arrivalCityId, departureAirportCode, arrivalAirportCode, start, end)
                .stream()
                .map(FlightSearchItemResponse::from)
                .collect(Collectors.toList());
        return ApiResponse.success(data);
    }

    @GetMapping("/detail")
    public ApiResponse<FlightSearchItemResponse> detail(@RequestParam Integer segmentId) {
        FlightSegment segment = segmentRepository.findById(segmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
        return ApiResponse.success(FlightSearchItemResponse.from(segment));
    }

    @GetMapping("/segments")
    public ApiResponse<List<FlightSearchItemResponse>> segments(@RequestParam Integer flightId) {
        return ApiResponse.success(segmentRepository.findByFlightFlightId(flightId)
                .stream()
                .map(FlightSearchItemResponse::from)
                .collect(Collectors.toList()));
    }
}
