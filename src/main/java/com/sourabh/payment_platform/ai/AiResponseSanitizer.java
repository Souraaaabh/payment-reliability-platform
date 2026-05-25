package com.sourabh.payment_platform.ai;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class AiResponseSanitizer {

    public FailureAnalysisResponse sanitizeFailureAnalysis(FailureAnalysisResponse response, String transactionId, String failureReason) {
        return new FailureAnalysisResponse(
                defaultText(response.transactionId(), transactionId),
                defaultText(response.analysis(), buildFailureAnalysis(failureReason)),
                normalizeSeverity(response.severity(), defaultSeverityForFailure(failureReason)),
                defaultText(response.possibleCause(), buildPossibleCause(failureReason)),
                defaultText(response.suggestedAction(), buildSuggestedAction(failureReason)),
                defaultText(response.downstreamIssue(), buildDownstreamIssue(failureReason)),
                defaultText(response.operationalRecommendation(), buildOperationalRecommendation(failureReason))
        );
    }

    public IncidentReportResponse sanitizeIncidentReport(IncidentReportResponse response, String impactSummary, String rootCauseSummary) {
        return new IncidentReportResponse(
                defaultText(response.incidentSummary(), "Payment incident detected in the requested time window."),
                defaultText(response.impact(), impactSummary),
                defaultText(response.rootCause(), rootCauseSummary),
                defaultText(response.affectedSystems(), "Payment consumer, downstream bank integration, and payment status update path."),
                defaultText(response.recommendation(), "Review failure distribution and prioritize downstream bank instability separately from business declines.")
        );
    }

    public LogSummaryResponse sanitizeLogSummary(LogSummaryResponse response) {
        return new LogSummaryResponse(
                defaultText(response.summary(), "Operational log summary generated from supplied payment platform logs."),
                defaultText(response.keyIssues(), "Review warnings, errors, and retry behavior in the supplied log set."),
                defaultText(response.frequentFailures(), "No dominant failure keyword was extracted from the supplied logs."),
                defaultText(response.criticalObservations(), "Focus on repeated timeout, retry, and payment failure patterns."),
                defaultText(response.recommendedFocusArea(), "Inspect downstream bank dependency behavior and async consumer failures.")
        );
    }

    public OperationalQueryResponse sanitizeOperationalQuery(OperationalQueryResponse response, String question, String evidence, String defaultSeverity) {
        return new OperationalQueryResponse(
                defaultText(response.question(), question),
                defaultText(response.answer(), "Operational analysis completed from recent payment failure data."),
                defaultText(response.supportingEvidence(), evidence),
                defaultText(response.recommendedAction(), "Review the recent failed payment distribution and downstream dependency behavior."),
                normalizeSeverity(response.severity(), defaultSeverity)
        );
    }

    private String defaultSeverityForFailure(String failureReason) {
        if (failureReason == null) {
            return "MEDIUM";
        }

        return switch (failureReason.toUpperCase(Locale.ROOT)) {
            case "BANK_TIMEOUT", "BANK_UNREACHABLE" -> "HIGH";
            case "INSUFFICIENT_FUNDS", "ACCOUNT_FROZEN", "INVALID_ACCOUNT" -> "MEDIUM";
            default -> "MEDIUM";
        };
    }

    private String buildFailureAnalysis(String failureReason) {
        if (failureReason == null) {
            return "Payment failed and requires further operational review.";
        }

        return switch (failureReason.toUpperCase(Locale.ROOT)) {
            case "BANK_TIMEOUT" -> "Payment failed after downstream bank timeout behavior affected processing.";
            case "BANK_UNREACHABLE" -> "Payment failed because the downstream bank service was unreachable after resilience handling.";
            case "INSUFFICIENT_FUNDS" -> "Payment failed because the issuing account did not have sufficient available funds.";
            case "ACCOUNT_FROZEN" -> "Payment failed because the customer account appears restricted or frozen.";
            case "INVALID_ACCOUNT" -> "Payment failed because the destination or source account details were rejected.";
            default -> "Payment failed with the provided bank failure reason and requires operational review.";
        };
    }

    private String buildPossibleCause(String failureReason) {
        if (failureReason == null) {
            return "Insufficient evidence to determine a single cause.";
        }

        return switch (failureReason.toUpperCase(Locale.ROOT)) {
            case "BANK_TIMEOUT" -> "External bank latency spike or degraded downstream bank processing.";
            case "BANK_UNREACHABLE" -> "Downstream bank connectivity or availability issue.";
            case "INSUFFICIENT_FUNDS" -> "Customer account balance was below the requested payment amount.";
            case "ACCOUNT_FROZEN" -> "Bank-side account restrictions blocked the payment.";
            case "INVALID_ACCOUNT" -> "Account validation failed at the downstream bank.";
            default -> "The bank response indicates a payment processing failure condition.";
        };
    }

    private String buildSuggestedAction(String failureReason) {
        if (failureReason == null) {
            return "Review payment details and recent downstream bank behavior before retrying.";
        }

        return switch (failureReason.toUpperCase(Locale.ROOT)) {
            case "BANK_TIMEOUT", "BANK_UNREACHABLE" -> "Retry after a short cooldown and inspect downstream bank health before bulk reprocessing.";
            case "INSUFFICIENT_FUNDS" -> "Notify the customer or operator to retry only after funds are available.";
            case "ACCOUNT_FROZEN" -> "Ask the customer to resolve account restrictions with the bank before retrying.";
            case "INVALID_ACCOUNT" -> "Validate account details before another payment attempt.";
            default -> "Review the failure reason and validate whether retry is appropriate.";
        };
    }

    private String buildDownstreamIssue(String failureReason) {
        if (failureReason == null) {
            return "Downstream payment dependency contribution is unclear from the available evidence.";
        }

        return switch (failureReason.toUpperCase(Locale.ROOT)) {
            case "BANK_TIMEOUT" -> "Downstream bank timeout behavior impacted the payment path.";
            case "BANK_UNREACHABLE" -> "Downstream bank availability failure impacted the payment path.";
            case "INSUFFICIENT_FUNDS", "ACCOUNT_FROZEN", "INVALID_ACCOUNT" -> "This appears to be a business or account-state rejection rather than a platform outage.";
            default -> "The downstream bank returned a failure condition.";
        };
    }

    private String buildOperationalRecommendation(String failureReason) {
        if (failureReason == null) {
            return "Monitor failure distribution and separate business declines from platform degradation.";
        }

        return switch (failureReason.toUpperCase(Locale.ROOT)) {
            case "BANK_TIMEOUT", "BANK_UNREACHABLE" -> "Track timeout and circuit breaker events and correlate with downstream bank latency.";
            case "INSUFFICIENT_FUNDS" -> "Treat this as a business decline pattern, not an infrastructure incident, unless the rate spikes abnormally.";
            case "ACCOUNT_FROZEN", "INVALID_ACCOUNT" -> "Track customer or account-state rejection trends separately from infrastructure failures.";
            default -> "Review the failure mix and separate infrastructure issues from business declines.";
        };
    }

    private String normalizeSeverity(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOW", "MEDIUM", "HIGH", "CRITICAL" -> normalized;
            default -> defaultValue;
        };
    }

    private String defaultText(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }
}
