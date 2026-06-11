package com.example.airticket.controller;

import com.example.airticket.common.ApiResponse;
import com.example.airticket.dto.request.ChangeApplyRequest;
import com.example.airticket.dto.request.ChangePayRequest;
import com.example.airticket.dto.request.CreateTicketRequest;
import com.example.airticket.dto.request.PayTicketRequest;
import com.example.airticket.dto.request.RefundTicketRequest;
import com.example.airticket.dto.response.TicketResponse;
import com.example.airticket.repository.TicketSaleRepository;
import com.example.airticket.service.TicketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ticket")
public class TicketController {
    private final TicketService ticketService;
    private final TicketSaleRepository ticketRepository;

    public TicketController(TicketService ticketService, TicketSaleRepository ticketRepository) {
        this.ticketService = ticketService;
        this.ticketRepository = ticketRepository;
    }

    @PostMapping({"/create", "/book"})
    public ApiResponse<TicketResponse> create(@RequestBody CreateTicketRequest request) {
        return ApiResponse.success(TicketResponse.from(ticketService.createTicket(request)));
    }

    @PostMapping("/pay")
    public ApiResponse<TicketResponse> pay(@RequestBody PayTicketRequest request) {
        return ApiResponse.success(TicketResponse.from(ticketService.payTicket(request)));
    }

    @GetMapping("/detail")
    public ApiResponse<TicketResponse> detail(@RequestParam Integer ticketId) {
        return ApiResponse.success(TicketResponse.from(ticketService.detail(ticketId)));
    }

    @GetMapping({"/my", "/list-by-user"})
    public ApiResponse<List<TicketResponse>> my(@RequestParam Integer userId) {
        return ApiResponse.success(ticketService.listByUser(userId)
                .stream()
                .map(TicketResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/refund")
    public ApiResponse<TicketResponse> refund(@RequestBody RefundTicketRequest request) {
        return ApiResponse.success(TicketResponse.from(ticketService.refundTicket(request)));
    }

    @PostMapping({"/change/apply", "/change"})
    public ApiResponse<TicketResponse> changeApply(@RequestBody ChangeApplyRequest request) {
        return ApiResponse.success(TicketResponse.from(ticketService.applyChange(request)));
    }

    @PostMapping("/change/pay")
    public ApiResponse<TicketResponse> changePay(@RequestBody ChangePayRequest request) {
        return ApiResponse.success(TicketResponse.from(ticketService.payChange(request)));
    }

    @GetMapping("/change/history")
    public ApiResponse<List<TicketResponse>> changeHistory(@RequestParam Integer ticketId) {
        return ApiResponse.success(ticketRepository.findByOriginalTicketTicketId(ticketId)
                .stream()
                .map(TicketResponse::from)
                .collect(Collectors.toList()));
    }

    @PostMapping("/expire-process")
    public ApiResponse<Integer> expireProcess() {
        return ApiResponse.success(ticketService.expirePendingOrders());
    }
}
