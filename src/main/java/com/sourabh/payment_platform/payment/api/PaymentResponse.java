package com.sourabh.payment_platform.payment.api;

import com.sourabh.payment_platform.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String transactionId,
        String userId,
        BigDecimal amount,
        PaymentStatus status,
        String failureReason,
        String idempotencyKey,
        Instant createdAt,
        Instant updatedAt
) {
}
