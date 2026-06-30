package com.example.airticket.service;

import com.example.airticket.dto.request.AircraftSaveRequest;
import com.example.airticket.dto.request.AirportSaveRequest;
import com.example.airticket.dto.request.CitySaveRequest;
import com.example.airticket.dto.request.FlightSaveRequest;
import com.example.airticket.dto.request.MealSaveRequest;
import com.example.airticket.dto.request.SegmentSaveRequest;
import com.example.airticket.entity.Aircraft;
import com.example.airticket.entity.Airport;
import com.example.airticket.entity.City;
import com.example.airticket.entity.Flight;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.entity.MealOption;
import com.example.airticket.enums.FlightStatus;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.AircraftRepository;
import com.example.airticket.repository.AirportRepository;
import com.example.airticket.repository.CityRepository;
import com.example.airticket.repository.FlightRepository;
import com.example.airticket.repository.FlightSegmentRepository;
import com.example.airticket.repository.MealOptionRepository;
import com.example.airticket.repository.TicketSaleRepository;
import com.example.airticket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final CityRepository cityRepository;
    private final AirportRepository airportRepository;
    private final AircraftRepository aircraftRepository;
    private final FlightRepository flightRepository;
    private final FlightSegmentRepository segmentRepository;
    private final MealOptionRepository mealRepository;
    private final TicketSaleRepository ticketRepository;

    public AdminService(UserRepository userRepository,
                        CityRepository cityRepository,
                        AirportRepository airportRepository,
                        AircraftRepository aircraftRepository,
                        FlightRepository flightRepository,
                        FlightSegmentRepository segmentRepository,
                        MealOptionRepository mealRepository,
                        TicketSaleRepository ticketRepository) {
        this.userRepository = userRepository;
        this.cityRepository = cityRepository;
        this.airportRepository = airportRepository;
        this.aircraftRepository = aircraftRepository;
        this.flightRepository = flightRepository;
        this.segmentRepository = segmentRepository;
        this.mealRepository = mealRepository;
        this.ticketRepository = ticketRepository;
    }

    public List<?> users() {
        return userRepository.findAll();
    }

    public Object user(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(40401, "用户不存在"));
    }

    public List<City> cities() {
        return cityRepository.findAll();
    }

    public City city(Integer cityId) {
        return cityRepository.findById(cityId)
                .orElseThrow(() -> new BusinessException(40402, "城市不存在"));
    }

    @Transactional
    public City saveCity(CitySaveRequest request) {
        City city = request.cityId == null ? new City() : cityRepository.findById(request.cityId)
                .orElseThrow(() -> new BusinessException(40402, "城市不存在"));
        city.cityName = request.cityName;
        city.cityCode = request.cityCode;
        city.country = request.country;
        return cityRepository.save(city);
    }

    public List<Airport> airports(Integer cityId) {
        return cityId == null ? airportRepository.findAll() : airportRepository.findByCityCityId(cityId);
    }

    public Airport airport(String airportCode) {
        return airportRepository.findById(airportCode)
                .orElseThrow(() -> new BusinessException(40403, "机场不存在"));
    }

    @Transactional
    public Airport saveAirport(AirportSaveRequest request) {
        Airport airport = request.airportCode == null ? new Airport() : airportRepository.findById(request.airportCode).orElse(new Airport());
        City city = cityRepository.findById(request.cityId)
                .orElseThrow(() -> new BusinessException(40402, "城市不存在"));
        airport.airportCode = request.airportCode;
        airport.airportName = request.airportName;
        airport.city = city;
        airport.isInternational = Boolean.TRUE.equals(request.isInternational);
        return airportRepository.save(airport);
    }

    public List<Aircraft> aircraft() {
        return aircraftRepository.findAll();
    }

    public Aircraft aircraft(String aircraftRegNo) {
        return aircraftRepository.findById(aircraftRegNo)
                .orElseThrow(() -> new BusinessException(40404, "飞机不存在"));
    }

    @Transactional
    public Aircraft saveAircraft(AircraftSaveRequest request) {
        Aircraft aircraft = request.aircraftRegNo == null ? new Aircraft() : aircraftRepository.findById(request.aircraftRegNo).orElse(new Aircraft());
        aircraft.aircraftRegNo = request.aircraftRegNo;
        aircraft.aircraftType = request.aircraftType;
        aircraft.manufacturer = request.manufacturer;
        aircraft.totalFirstClassSeats = request.totalFirstClassSeats;
        aircraft.totalEconomySeats = request.totalEconomySeats;
        aircraft.status = request.status;
        aircraft.remark = request.remark;
        return aircraftRepository.save(aircraft);
    }

    public List<Flight> flights() {
        return flightRepository.findAll();
    }

    public Flight flight(Integer flightId) {
        return flightRepository.findById(flightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
    }

    @Transactional
    public Flight saveFlight(FlightSaveRequest request) {
        Flight flight = request.flightId == null ? new Flight() : flightRepository.findById(request.flightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
        if (flight.flightId != null && ticketRepository.countByFlightFlightId(flight.flightId) > 0) {
            flight.flightStatus = parseFlightStatus(request.flightStatus);
            flight.remark = request.remark;
            Flight saved = flightRepository.save(flight);
            log.info("admin.saveFlightStatus flightId={} status={}", saved.flightId, saved.flightStatus);
            return saved;
        }
        Aircraft aircraft = aircraftRepository.findById(request.aircraftRegNo)
                .orElseThrow(() -> new BusinessException(40404, "飞机不存在"));
        airportRepository.findById(request.departureAirportCode)
                .orElseThrow(() -> new BusinessException(40403, "机场不存在"));
        airportRepository.findById(request.arrivalAirportCode)
                .orElseThrow(() -> new BusinessException(40403, "机场不存在"));
        flight.flightNumber = request.flightNumber;
        flight.flightDate = request.flightDate;
        flight.aircraft = aircraft;
        flight.flightStatus = parseFlightStatus(request.flightStatus);
        flight.departureAirportCode = request.departureAirportCode;
        flight.arrivalAirportCode = request.arrivalAirportCode;
        flight.remark = request.remark;
        Flight saved = flightRepository.save(flight);
        log.info("admin.saveFlight flightId={} flightNumber={} status={}", saved.flightId, saved.flightNumber, saved.flightStatus);
        return saved;
    }

    @Transactional
    public Flight disableFlight(Integer flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
        flight.flightStatus = FlightStatus.DISABLED;
        log.info("admin.disableFlight flightId={}", flightId);
        return flight;
    }

    @Transactional
    public Flight enableFlight(Integer flightId) {
        Flight flight = flightRepository.findById(flightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
        flight.flightStatus = FlightStatus.NORMAL;
        log.info("admin.enableFlight flightId={}", flightId);
        return flight;
    }

    public List<FlightSegment> segments(Integer flightId) {
        return flightId == null ? segmentRepository.findAll() : segmentRepository.findByFlightFlightId(flightId);
    }

    public FlightSegment segment(Integer segmentId) {
        return segmentRepository.findById(segmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
    }

    @Transactional
    public FlightSegment saveSegment(SegmentSaveRequest request) {
        FlightSegment segment = request.segmentId == null ? new FlightSegment() : segmentRepository.findById(request.segmentId)
                .orElseThrow(() -> new BusinessException(40406, "航段不存在"));
        Flight flight = flightRepository.findById(request.flightId)
                .orElseThrow(() -> new BusinessException(40405, "航班不存在"));
        if (request.originStopNo >= request.destinationStopNo) {
            throw new BusinessException(41007, "起止站序错误");
        }
        segmentRepository.findByFlightFlightIdAndOriginStopNoAndDestinationStopNo(
                        request.flightId, request.originStopNo, request.destinationStopNo)
                .filter(existing -> request.segmentId == null || !existing.segmentId.equals(request.segmentId))
                .ifPresent(existing -> {
                    throw new BusinessException(41010, "航段站序已存在，请修改起止站序或改为编辑已有航段");
                });
        airportRepository.findById(request.originAirportCode)
                .orElseThrow(() -> new BusinessException(40409, "起飞机场不存在"));
        airportRepository.findById(request.destinationAirportCode)
                .orElseThrow(() -> new BusinessException(40410, "到达机场不存在"));
        segment.flight = flight;
        segment.originStopNo = request.originStopNo;
        segment.destinationStopNo = request.destinationStopNo;
        segment.originAirportCode = request.originAirportCode;
        segment.destinationAirportCode = request.destinationAirportCode;
        segment.plannedDepartureTime = request.plannedDepartureTime;
        segment.plannedArrivalTime = request.plannedArrivalTime;
        segment.actualDepartureTime = request.actualDepartureTime;
        segment.actualArrivalTime = request.actualArrivalTime;
        segment.delayMinutes = request.delayMinutes == null ? 0 : request.delayMinutes;
        segment.delayReason = request.delayReason;
        segment.firstClassRemainingSeats = request.firstClassRemainingSeats;
        segment.economyRemainingSeats = request.economyRemainingSeats;
        segment.firstClassPrice = request.firstClassPrice;
        segment.economyPrice = request.economyPrice;
        segment.isSpecialOffer = Boolean.TRUE.equals(request.isSpecialOffer);
        segment.remark = request.remark;
        FlightSegment saved = segmentRepository.save(segment);
        log.info("admin.saveSegment segmentId={} flightId={} specialOffer={}", saved.segmentId, flight.flightId, saved.isSpecialOffer);
        return saved;
    }

    public List<MealOption> meals() {
        return mealRepository.findAll();
    }

    public MealOption meal(Integer mealId) {
        return mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(40408, "餐食不存在"));
    }

    @Transactional
    public MealOption saveMeal(MealSaveRequest request) {
        MealOption meal = request.mealId == null ? new MealOption() : mealRepository.findById(request.mealId)
                .orElseThrow(() -> new BusinessException(40408, "餐食不存在"));
        meal.mealName = request.mealName;
        meal.mealType = request.mealType;
        meal.isAvailable = request.isAvailable == null || request.isAvailable;
        meal.description = request.description;
        return mealRepository.save(meal);
    }

    @Transactional
    public MealOption disableMeal(Integer mealId) {
        MealOption meal = mealRepository.findById(mealId)
                .orElseThrow(() -> new BusinessException(40408, "餐食不存在"));
        meal.isAvailable = false;
        return meal;
    }

    public Map<String, Object> dashboardSummary() {
        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userRepository.count());
        data.put("flightCount", flightRepository.count());
        data.put("segmentCount", segmentRepository.count());
        data.put("ticketCount", ticketRepository.count());
        data.put("mealCount", mealRepository.count());
        return data;
    }

    private FlightStatus parseFlightStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return FlightStatus.NORMAL;
        }
        return FlightStatus.valueOf(status);
    }
}
