package com.example.airticket.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public final class OrderNoGenerator {
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoGenerator() {
    }

    public static String next() {
        int value = SEQUENCE.updateAndGet(current -> current >= 9999 ? 1 : current + 1);
        return "ORD" + LocalDateTime.now().format(FORMATTER) + String.format("%04d", value);
    }
}
