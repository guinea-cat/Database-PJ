package com.example.airticket.scheduler;

import com.example.airticket.service.ExpiredOrderService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TicketExpireScheduler {
    private final ExpiredOrderService expiredOrderService;

    public TicketExpireScheduler(ExpiredOrderService expiredOrderService) {
        this.expiredOrderService = expiredOrderService;
    }

    @Scheduled(fixedDelay = 60000)
    public void scanExpiredOrders() {
        expiredOrderService.processExpiredOrders();
    }
}
