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
public class IncidentReportService {

    private final PaymentRepository paymentRepository;
    private final AiPromptFactory aiPromptFactory;
    private final AiStructuredOperationsService aiStructuredOperationsService;
    private final AiResponseSanitizer aiResponseSanitizer;

    public IncidentReportService(
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
    public IncidentReportResponse generateIncidentReport(int minutes) {
        Instant from = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        List<Payment> recentFailures = paymentRepository
                .findAllByStatusAndUpdatedAtAfterOrderByUpdatedAtDesc(PaymentStatus.FAILED, from);
        long failedPayments = recentFailures.size();
        long totalPayments = paymentRepository.countByUpdatedAtAfter(from);
        List<PaymentFailureReasonCount> reasonCounts = paymentRepository.summarizeFailureReasonsSince(from);

        if (failedPayments == 0L) {
            return new IncidentReportResponse(
                    "No payment incident detected in the requested time window.",
                    "0 failed payments observed.",
                    "No active failure pattern detected.",
                    "Payment consumer, bank integration, and cache layer appear stable.",
                    "Continue normal monitoring."
            );
        }

        IncidentReportResponse response = aiStructuredOperationsService.executeStructuredOperation(
                "generate-incident-report",
                aiPromptFactory.incidentReportSystemPrompt(),
                aiPromptFactory.incidentReportUserPrompt(minutes, failedPayments, totalPayments, reasonCounts, recentFailures),
                IncidentReportResponse.class
        );

        String impactSummary = failedPayments + " failed payments observed out of " + totalPayments
                + " payments in the last " + minutes + " minutes.";
        String rootCauseSummary = reasonCounts.isEmpty()
                ? "Failure reasons were present but no grouped reason summary was generated."
                : reasonCounts.stream()
                .limit(3)
                .map(reasonCount -> (reasonCount.getFailureReason() == null ? "UNKNOWN" : reasonCount.getFailureReason())
                        + "=" + reasonCount.getFailureCount())
                .reduce((left, right) -> left + ", " + right)
                .orElse("Grouped failure reason summary unavailable.");

        return aiResponseSanitizer.sanitizeIncidentReport(response, impactSummary, rootCauseSummary);
    }
}
