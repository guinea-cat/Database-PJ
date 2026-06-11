package com.example.airticket.controller;

import com.example.airticket.common.ApiResponse;
import com.example.airticket.dto.request.AircraftSaveRequest;
import com.example.airticket.dto.request.AirportSaveRequest;
import com.example.airticket.dto.request.CitySaveRequest;
import com.example.airticket.dto.request.FlightSaveRequest;
import com.example.airticket.dto.request.IdRequest;
import com.example.airticket.dto.request.MealSaveRequest;
import com.example.airticket.dto.request.SegmentSaveRequest;
import com.example.airticket.dto.response.TicketResponse;
import com.example.airticket.entity.Aircraft;
import com.example.airticket.entity.Airport;
import com.example.airticket.entity.City;
import com.example.airticket.entity.Flight;
import com.example.airticket.entity.FlightSegment;
import com.example.airticket.entity.MealOption;
import com.example.airticket.exception.BusinessException;
import com.example.airticket.repository.TicketSaleRepository;
import com.example.airticket.service.AdminService;
import com.example.airticket.service.ExpiredOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final TicketSaleRepository ticketRepository;
    private final ExpiredOrderService expiredOrderService;

    public AdminController(AdminService adminService,
                           TicketSaleRepository ticketRepository,
                           ExpiredOrderService expiredOrderService) {
        this.adminService = adminService;
        this.ticketRepository = ticketRepository;
        this.expiredOrderService = expiredOrderService;
    }

    @GetMapping("/user/list")
    public ApiResponse<List<?>> userList() {
        return ApiResponse.success(adminService.users());
    }

    @GetMapping("/user/detail")
    public ApiResponse<?> userDetail(@RequestParam Integer userId) {
        return ApiResponse.success(adminService.user(userId));
    }

    @GetMapping("/city/list")
    public ApiResponse<List<City>> cityList() {
        return ApiResponse.success(adminService.cities());
    }

    @GetMapping("/city/detail")
    public ApiResponse<City> cityDetail(@RequestParam Integer cityId) {
        return ApiResponse.success(adminService.city(cityId));
    }

    @PostMapping({"/city/add", "/city/update"})
    public ApiResponse<City> citySave(@RequestBody CitySaveRequest request) {
        return ApiResponse.success(adminService.saveCity(request));
    }

    @PostMapping("/city/disable")
    public ApiResponse<Void> cityDisable() {
        throw new BusinessException(40004, "City 表没有停用字段，ER-first 版本不支持持久化停用城市");
    }

    @GetMapping("/airport/list")
    public ApiResponse<List<Airport>> airportList(@RequestParam(required = false) Integer cityId) {
        return ApiResponse.success(adminService.airports(cityId));
    }

    @GetMapping("/airport/detail")
    public ApiResponse<Airport> airportDetail(@RequestParam String airportCode) {
        return ApiResponse.success(adminService.airport(airportCode));
    }

    @PostMapping({"/airport/add", "/airport/update"})
    public ApiResponse<Airport> airportSave(@RequestBody AirportSaveRequest request) {
        return ApiResponse.success(adminService.saveAirport(request));
    }

    @PostMapping("/airport/disable")
    public ApiResponse<Void> airportDisable() {
        throw new BusinessException(40004, "Airport 表没有停用字段，ER-first 版本不支持持久化停用机场");
    }

    @GetMapping("/aircraft/list")
    public ApiResponse<List<Aircraft>> aircraftList() {
        return ApiResponse.success(adminService.aircraft());
    }

    @GetMapping("/aircraft/detail")
    public ApiResponse<Aircraft> aircraftDetail(@RequestParam String aircraftRegNo) {
        return ApiResponse.success(adminService.aircraft(aircraftRegNo));
    }

    @PostMapping({"/aircraft/add", "/aircraft/update"})
    public ApiResponse<Aircraft> aircraftSave(@RequestBody AircraftSaveRequest request) {
        return ApiResponse.success(adminService.saveAircraft(request));
    }

    @PostMapping("/aircraft/disable")
    public ApiResponse<Aircraft> aircraftDisable(@RequestBody IdRequest request) {
        AircraftSaveRequest save = new AircraftSaveRequest();
        Aircraft existing = adminService.aircraft().stream()
                .filter(aircraft -> aircraft.aircraftRegNo.equals(request.aircraftRegNo))
                .findFirst()
                .orElseThrow(() -> new BusinessException(40404, "飞机不存在"));
        save.aircraftRegNo = existing.aircraftRegNo;
        save.aircraftType = existing.aircraftType;
        save.manufacturer = existing.manufacturer;
        save.totalFirstClassSeats = existing.totalFirstClassSeats;
        save.totalEconomySeats = existing.totalEconomySeats;
        save.status = "DISABLED";
        save.remark = existing.remark;
        return ApiResponse.success(adminService.saveAircraft(save));
    }

    @GetMapping("/flight/list")
    public ApiResponse<List<Flight>> flightList() {
        return ApiResponse.success(adminService.flights());
    }

    @GetMapping("/flight/detail")
    public ApiResponse<Flight> flightDetail(@RequestParam Integer flightId) {
        return ApiResponse.success(adminService.flight(flightId));
    }

    @PostMapping({"/flight/add", "/flight/update"})
    public ApiResponse<Flight> flightSave(@RequestBody FlightSaveRequest request) {
        return ApiResponse.success(adminService.saveFlight(request));
    }

    @PostMapping("/flight/disable")
    public ApiResponse<Flight> flightDisable(@RequestBody IdRequest request) {
        return ApiResponse.success(adminService.disableFlight(request.flightId));
    }

    @PostMapping("/flight/enable")
    public ApiResponse<Flight> flightEnable(@RequestBody IdRequest request) {
        return ApiResponse.success(adminService.enableFlight(request.flightId));
    }

    @GetMapping("/segment/list")
    public ApiResponse<List<FlightSegment>> segmentList(@RequestParam(required = false) Integer flightId) {
        return ApiResponse.success(adminService.segments(flightId));
    }

    @GetMapping("/segment/detail")
    public ApiResponse<FlightSegment> segmentDetail(@RequestParam Integer segmentId) {
        return ApiResponse.success(adminService.segment(segmentId));
    }

    @PostMapping({"/segment/add", "/segment/update"})
    public ApiResponse<FlightSegment> segmentSave(@RequestBody SegmentSaveRequest request) {
        return ApiResponse.success(adminService.saveSegment(request));
    }

    @PostMapping("/segment/disable")
    public ApiResponse<Void> segmentDisable() {
        throw new BusinessException(40004, "FlightSegment 表没有停用字段，ER-first 版本不支持持久化停用航段");
    }

    @GetMapping("/meal/list")
    public ApiResponse<List<MealOption>> mealList() {
        return ApiResponse.success(adminService.meals());
    }

    @GetMapping("/meal/detail")
    public ApiResponse<MealOption> mealDetail(@RequestParam Integer mealId) {
        return ApiResponse.success(adminService.meal(mealId));
    }

    @PostMapping({"/meal/add", "/meal/update"})
    public ApiResponse<MealOption> mealSave(@RequestBody MealSaveRequest request) {
        return ApiResponse.success(adminService.saveMeal(request));
    }

    @PostMapping("/meal/disable")
    public ApiResponse<MealOption> mealDisable(@RequestBody IdRequest request) {
        return ApiResponse.success(adminService.disableMeal(request.mealId));
    }

    @GetMapping("/ticket/list")
    public ApiResponse<List<TicketResponse>> ticketList() {
        return ApiResponse.success(ticketRepository.findAll().stream()
                .map(TicketResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/ticket/detail")
    public ApiResponse<TicketResponse> ticketDetail(@RequestParam Integer ticketId) {
        return ApiResponse.success(ticketRepository.findById(ticketId)
                .map(TicketResponse::from)
                .orElseThrow(() -> new BusinessException(40407, "订单不存在")));
    }

    @GetMapping({"/refund/list", "/change/list"})
    public ApiResponse<List<TicketResponse>> statusRecordList() {
        return ApiResponse.success(ticketRepository.findAll().stream()
                .map(TicketResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/dashboard/summary")
    public ApiResponse<Map<String, Object>> dashboardSummary() {
        return ApiResponse.success(adminService.dashboardSummary());
    }

    @GetMapping("/dashboard/statistics")
    public ApiResponse<Map<String, Object>> dashboardStatistics() {
        return ApiResponse.success(adminService.dashboardSummary());
    }

    @PostMapping("/job/expire-order")
    public ApiResponse<Integer> expireOrder() {
        return ApiResponse.success(expiredOrderService.processExpiredOrders());
    }
}
