package com.sourabh.payment_platform.ai;

public record LogSummaryResponse(
        String summary,
        String keyIssues,
        String frequentFailures,
        String criticalObservations,
        String recommendedFocusArea
) {
}
