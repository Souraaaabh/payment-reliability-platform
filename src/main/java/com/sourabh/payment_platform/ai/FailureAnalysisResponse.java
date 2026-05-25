package com.sourabh.payment_platform.ai;

public record FailureAnalysisResponse(
        String transactionId,
        String analysis,
        String severity,
        String possibleCause,
        String suggestedAction,
        String downstreamIssue,
        String operationalRecommendation
) {
}
