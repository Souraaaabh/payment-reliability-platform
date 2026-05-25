package com.sourabh.payment_platform.ai;

import com.sourabh.payment_platform.payment.domain.Payment;
import com.sourabh.payment_platform.payment.domain.PaymentRepository;
import com.sourabh.payment_platform.shared.exception.PaymentNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FailureAnalysisService {

    private final PaymentRepository paymentRepository;
    private final AiPromptFactory aiPromptFactory;
    private final AiStructuredOperationsService aiStructuredOperationsService;
    private final AiResponseSanitizer aiResponseSanitizer;
    private final int configuredRetryAttempts;

    public FailureAnalysisService(
            PaymentRepository paymentRepository,
            AiPromptFactory aiPromptFactory,
            AiStructuredOperationsService aiStructuredOperationsService,
            AiResponseSanitizer aiResponseSanitizer,
            @Value("${resilience4j.retry.instances.bankApiRetry.max-attempts:3}") int configuredRetryAttempts
    ) {
        this.paymentRepository = paymentRepository;
        this.aiPromptFactory = aiPromptFactory;
        this.aiStructuredOperationsService = aiStructuredOperationsService;
        this.aiResponseSanitizer = aiResponseSanitizer;
        this.configuredRetryAttempts = configuredRetryAttempts;
    }

    @Transactional(readOnly = true)
    public FailureAnalysisResponse analyzeFailure(String transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new PaymentNotFoundException(transactionId));

        FailureAnalysisResponse response = aiStructuredOperationsService.executeStructuredOperation(
                "analyze-failure",
                aiPromptFactory.failureAnalysisSystemPrompt(),
                aiPromptFactory.failureAnalysisUserPrompt(payment, configuredRetryAttempts),
                FailureAnalysisResponse.class
        );

        return aiResponseSanitizer.sanitizeFailureAnalysis(response, payment.getTransactionId(), payment.getFailureReason());
    }
}
