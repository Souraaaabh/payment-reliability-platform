package com.sourabh.payment_platform.payment.client;

import java.math.BigDecimal;

public interface BankApiClient {

    BankApiResponse processPayment(String transactionId, BigDecimal amount, String userId);
}
