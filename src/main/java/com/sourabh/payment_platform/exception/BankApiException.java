package com.sourabh.payment_platform.exception;

public class BankApiException extends RuntimeException {

    public BankApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public BankApiException(String message) {
        super(message);
    }
}
