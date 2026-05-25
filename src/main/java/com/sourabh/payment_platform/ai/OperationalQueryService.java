package com.sourabh.payment_platform.ai;

import com.sourabh.payment_platform.payment.domain.Payment;
import com.sourabh.payment_platform.payment.domain.PaymentFailureReasonCount;
import com.sourabh.payment_platform.payment.domain.PaymentRepository;
import com.sourabh.payment_platform.payment.domain.PaymentStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperationalQueryService {

    private final PaymentRepository paymentRepository;
    private final AiPromptFactory aiPromptFactory;
    private final AiStructuredOperationsService aiStructuredOperationsService;
    private final AiResponseSanitizer aiResponseSanitizer;

    public OperationalQueryService(
            PaymentRepository paymentRepository,
            AiPromptFactory aiPromptFactory,
            AiStructuredOperationsService aiStructuredOperationsService,
            AiResponseSanitizer aiResponseSanitizer
    ) {
        this.paymentRepository = paymentRepository;
        this.aiPromptFactory = aiPromptFactory;
        this.aiStructuredOperationsService = aiStructuredOperationsService;
        this.aiResponseSanitizer = aiResponseSanitizer;
    }

    @Transactional(readOnly = true)
    public OperationalQueryResponse answerOperationalQuestion(String question) {
        int timeWindowMinutes = resolveTimeWindowMinutes(question);
        Instant from = Instant.now().minus(timeWindowMinutes, ChronoUnit.MINUTES);
        List<Payment> recentFailures = paymentRepository
                .findAllByStatusAndUpdatedAtAfterOrderByUpdatedAtDesc(PaymentStatus.FAILED, from);
        long failedPayments = recentFailures.size();
        long totalPayments = paymentRepository.countByUpdatedAtAfter(from);
        List<PaymentFailureReasonCount> reasonCounts = paymentRepository.summarizeFailureReasonsSince(from);

        if (failedPayments == 0L) {
            return new OperationalQueryResponse(
                    question,
                    "No payment failures were detected in the requested time window.",
                    "failedPayments=0, totalPaymentsObserved=" + totalPayments,
                    "Continue monitoring the payment consumer and downstream bank dependency.",
                    "LOW"
            );
        }

        OperationalQueryResponse response = aiStructuredOperationsService.executeStructuredOperation(
                "operational-query",
                aiPromptFactory.operationalQuerySystemPrompt(),
                aiPromptFactory.operationalQueryUserPrompt(question, timeWindowMinutes, failedPayments, totalPayments, reasonCounts, recentFailures),
                OperationalQueryResponse.class
        );

        String evidence = "failedPayments=" + failedPayments
                + ", totalPaymentsObserved=" + totalPayments
                + ", topFailureReasons="
                + reasonCounts.stream()
                .limit(3)
                .map(reasonCount -> (reasonCount.getFailureReason() == null ? "UNKNOWN" : reasonCount.getFailureReason())
                        + "=" + reasonCount.getFailureCount())
                .reduce((left, right) -> left + "; " + right)
                .orElse("none");

        String defaultSeverity = recentFailures.stream()
                .map(Payment::getFailureReason)
                .filter(reason -> reason != null && (reason.equalsIgnoreCase("BANK_TIMEOUT") || reason.equalsIgnoreCase("BANK_UNREACHABLE")))
                .findFirst()
                .map(reason -> "HIGH")
                .orElse("MEDIUM");

        return aiResponseSanitizer.sanitizeOperationalQuery(response, question, evidence, defaultSeverity);
    }

    private int resolveTimeWindowMinutes(String question) {
        String normalized = question.toLowerCase();

        if (normalized.contains("last hour")) {
            return 60;
        }
        if (normalized.contains("today")) {
            return 24 * 60;
        }
        if (normalized.contains("15 minutes")) {
            return 15;
        }
        if (normalized.contains("30 minutes")) {
            return 30;
        }

        return 120;
    }
}
