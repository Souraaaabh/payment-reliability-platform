package com.sourabh.payment_platform.ai;

import com.sourabh.payment_platform.payment.domain.Payment;
import com.sourabh.payment_platform.payment.domain.PaymentFailureReasonCount;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiPromptFactory {

    private final AiInputSanitizer aiInputSanitizer;

    public AiPromptFactory(AiInputSanitizer aiInputSanitizer) {
        this.aiInputSanitizer = aiInputSanitizer;
    }

    public String failureAnalysisSystemPrompt() {
        return """
                You are an SRE reliability analysis engine for a distributed payment reliability platform.
                The platform uses Spring Boot, Kafka, Redis, MySQL, and Resilience4j.
                Analyze payment failures with concise, production-oriented outputs.
                Return valid JSON only with fields:
                transactionId, analysis, severity, possibleCause, suggestedAction, downstreamIssue, operationalRecommendation.
                Keep severity to LOW, MEDIUM, HIGH, or CRITICAL.
                You must copy the exact provided transactionId into the response.
                Do not return null for transactionId, analysis, severity, possibleCause, suggestedAction, downstreamIssue, or operationalRecommendation.
                Distinguish business failures such as INSUFFICIENT_FUNDS from platform failures such as BANK_TIMEOUT or BANK_UNREACHABLE.
                Do not recommend increasing retry counts for business failures like insufficient funds.
                Avoid conversational filler.
                """;
    }

    public String failureAnalysisUserPrompt(Payment payment, int configuredRetryAttempts) {
        return """
                Analyze this payment reliability event.

                transactionId: %s
                paymentStatus: %s
                failureReason: %s
                createdAt: %s
                updatedAt: %s
                configuredRetryAttempts: %s
                observedRetryCount: unavailable in current persistence model
                bankResponse: %s

                If failureReason is INSUFFICIENT_FUNDS, treat it as a customer or account state issue, not an infrastructure outage.
                If failureReason is BANK_TIMEOUT or BANK_UNREACHABLE, treat it as a downstream dependency issue.
                Focus on probable root cause, downstream dependency behavior, severity, and the next operational action.
                """.formatted(
                aiInputSanitizer.sanitize(payment.getTransactionId()),
                payment.getStatus(),
                aiInputSanitizer.sanitize(nullSafe(payment.getFailureReason())),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                configuredRetryAttempts,
                aiInputSanitizer.sanitize(nullSafe(payment.getFailureReason()))
        );
    }

    public String incidentReportSystemPrompt() {
        return """
                You are an incident management assistant for a payment reliability platform.
                Convert payment failure metrics into a concise incident report for on-call engineers.
                Return valid JSON only with fields:
                incidentSummary, impact, rootCause, affectedSystems, recommendation.
                Do not return null fields.
                Use the supplied failure counts and failure rate in the impact statement.
                Separate business failure patterns from infrastructure failure patterns.
                Do not present insufficient funds alone as a platform outage.
                Keep it short, operational, and action-oriented.
                """;
    }

    public String incidentReportUserPrompt(
            int minutes,
            long failedPayments,
            long totalPayments,
            List<PaymentFailureReasonCount> reasonCounts,
            List<Payment> recentFailures
    ) {
        return """
                Generate an incident report for payment failures.

                timeWindowMinutes: %s
                failedPayments: %s
                totalPaymentsObserved: %s
                failureRatePercent: %.2f
                topFailureReasons:
                %s

                recentFailedTransactions:
                %s

                Mention impact with numeric evidence, likely downstream systems, and a practical remediation direction.
                """.formatted(
                minutes,
                failedPayments,
                totalPayments,
                calculateFailureRate(failedPayments, totalPayments),
                formatReasonCounts(reasonCounts),
                formatPayments(recentFailures)
        );
    }

    public String logSummarizationSystemPrompt() {
        return """
                You are an operational log summarization engine for a distributed payment platform.
                Convert raw backend logs into concise SRE-oriented insights.
                Return valid JSON only with fields:
                summary, keyIssues, frequentFailures, criticalObservations, recommendedFocusArea.
                Do not return null fields.
                Prefer concrete observations over generic statements.
                Keep it concise and production-focused.
                """;
    }

    public String logSummarizationUserPrompt(String rawLogs) {
        return """
                Summarize these sanitized operational logs:

                %s
                """.formatted(aiInputSanitizer.sanitize(rawLogs));
    }

    public String operationalQuerySystemPrompt() {
        return """
                You are an operational intelligence layer for a payment reliability platform.
                Answer operational questions using the supplied metrics only.
                Return valid JSON only with fields:
                question, answer, supportingEvidence, recommendedAction, severity.
                Do not return null fields.
                supportingEvidence must reference the provided counts, failure reasons, or recent failure examples.
                Keep severity to LOW, MEDIUM, HIGH, or CRITICAL.
                Separate business failures from downstream platform failures.
                Keep the response concise and reliability-focused.
                """;
    }

    public String operationalQueryUserPrompt(
            String question,
            int timeWindowMinutes,
            long failedPayments,
            long totalPayments,
            List<PaymentFailureReasonCount> reasonCounts,
            List<Payment> recentFailures
    ) {
        return """
                Answer this operational question using the supplied payment reliability context.

                question: %s
                timeWindowMinutes: %s
                failedPayments: %s
                totalPaymentsObserved: %s
                failureRatePercent: %.2f
                topFailureReasons:
                %s

                recentFailedTransactions:
                %s
                """.formatted(
                aiInputSanitizer.sanitize(question),
                timeWindowMinutes,
                failedPayments,
                totalPayments,
                calculateFailureRate(failedPayments, totalPayments),
                formatReasonCounts(reasonCounts),
                formatPayments(recentFailures)
        );
    }

    private String formatReasonCounts(List<PaymentFailureReasonCount> reasonCounts) {
        if (reasonCounts.isEmpty()) {
            return "- none";
        }

        return reasonCounts.stream()
                .limit(5)
                .map(reasonCount -> "- %s: %s".formatted(
                        aiInputSanitizer.sanitize(nullSafe(reasonCount.getFailureReason())),
                        reasonCount.getFailureCount()
                ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatPayments(List<Payment> payments) {
        if (payments.isEmpty()) {
            return "- none";
        }

        return payments.stream()
                .limit(10)
                .map(payment -> "- transactionId=%s, failureReason=%s, updatedAt=%s".formatted(
                        aiInputSanitizer.sanitize(payment.getTransactionId()),
                        aiInputSanitizer.sanitize(nullSafe(payment.getFailureReason())),
                        payment.getUpdatedAt()
                ))
                .collect(Collectors.joining(System.lineSeparator()));
    }

    private double calculateFailureRate(long failedPayments, long totalPayments) {
        if (totalPayments == 0L) {
            return 0.0;
        }

        return (failedPayments * 100.0) / totalPayments;
    }

    private String nullSafe(String value) {
        return value == null ? "NONE" : value;
    }
}
