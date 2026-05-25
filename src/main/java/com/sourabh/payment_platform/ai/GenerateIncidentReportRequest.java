package com.sourabh.payment_platform.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record GenerateIncidentReportRequest(
        @NotNull(message = "minutes is required")
        @Min(value = 1, message = "minutes must be at least 1")
        Integer minutes
) {
}
