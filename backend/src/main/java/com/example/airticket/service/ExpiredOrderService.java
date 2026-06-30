package com.example.airticket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExpiredOrderService {
    private static final Logger log = LoggerFactory.getLogger(ExpiredOrderService.class);

    private final TicketService ticketService;

    public ExpiredOrderService(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public int processExpiredOrders() {
        int count = ticketService.expirePendingOrders();
        log.info("job.expirePendingOrders count={}", count);
        return count;
    }
}
