package com.sourabh.payment_platform.shared.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String transactionId) {
        super("Payment not found for transactionId: " + transactionId);
    }
}
