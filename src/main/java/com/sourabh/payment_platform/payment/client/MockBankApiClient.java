package com.sourabh.payment_platform.payment.client;

import com.sourabh.payment_platform.exception.BankApiException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class MockBankApiClient implements BankApiClient {

    private static final List<String> FAILURE_REASONS = List.of(
            "INSUFFICIENT_FUNDS",
            "BANK_TIMEOUT",
            "ACCOUNT_FROZEN",
            "INVALID_ACCOUNT"
    );

    private final Random random = new Random();

    @Override
    public BankApiResponse processPayment(String transactionId, BigDecimal amount, String userId) {
        try {
            simulateNetworkDelay();

            boolean success = random.nextInt(100) < 70;
            if (success) {
                return new BankApiResponse(true, null);
            }

            String failureReason = FAILURE_REASONS.get(random.nextInt(FAILURE_REASONS.size()));
            return new BankApiResponse(false, failureReason);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BankApiException("Bank API call was interrupted", exception);
        }
    }

    private void simulateNetworkDelay() throws InterruptedException {
        int delayInMilliseconds = 200 + random.nextInt(601);
        Thread.sleep(delayInMilliseconds);
    }
}
