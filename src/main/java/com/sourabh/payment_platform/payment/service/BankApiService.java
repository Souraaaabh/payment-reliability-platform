package com.sourabh.payment_platform.payment.service;

import com.sourabh.payment_platform.payment.client.BankApiClient;
import com.sourabh.payment_platform.payment.client.BankApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankApiService {

    private final BankApiClient bankApiClient;

    @Retry(name = "bankApiRetry")
    @CircuitBreaker(name = "bankApiCircuitBreaker", fallbackMethod = "bankApiFallback")
    public BankApiResponse callBankWithResilience(String transactionId, BigDecimal amount, String userId) {
        log.info("Calling bank API for transactionId: {}", transactionId);
        return bankApiClient.processPayment(transactionId, amount, userId);
    }

    @SuppressWarnings("unused")
    public BankApiResponse bankApiFallback(String transactionId, BigDecimal amount, String userId, Throwable cause) {
        log.error("Bank API unreachable after retries for transactionId: {}, cause: {}", transactionId, cause.toString());
        return new BankApiResponse(false, "BANK_UNREACHABLE");
    }
}
