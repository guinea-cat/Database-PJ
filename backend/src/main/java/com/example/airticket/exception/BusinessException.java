package com.example.airticket.exception;

public class BusinessException extends RuntimeException {
    public final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
