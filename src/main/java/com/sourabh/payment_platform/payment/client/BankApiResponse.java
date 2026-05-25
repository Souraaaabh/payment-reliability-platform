package com.sourabh.payment_platform.payment.client;

public record BankApiResponse(
        boolean success,
        String failureReason
) {
}
