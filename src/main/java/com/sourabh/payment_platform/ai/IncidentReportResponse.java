package com.sourabh.payment_platform.ai;

public record IncidentReportResponse(
        String incidentSummary,
        String impact,
        String rootCause,
        String affectedSystems,
        String recommendation
) {
}
