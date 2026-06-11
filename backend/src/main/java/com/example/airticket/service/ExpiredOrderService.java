package com.example.airticket.service;

import org.springframework.stereotype.Service;

@Service
public class ExpiredOrderService {
    private final TicketService ticketService;

    public ExpiredOrderService(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public int processExpiredOrders() {
        return ticketService.expirePendingOrders();
    }
}
