package com.sourabh.payment_platform.shared.exception;

public class BankApiException extends RuntimeException {

    public BankApiException(String message) {
        super(message);
    }

    public BankApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
