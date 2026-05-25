package com.sourabh.payment_platform.ai;

public record OperationalQueryResponse(
        String question,
        String answer,
        String supportingEvidence,
        String recommendedAction,
        String severity
) {
}
