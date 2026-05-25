package com.sourabh.payment_platform.payment.event;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCreatedEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String idempotencyKey;
    private Instant createdAt;
}
