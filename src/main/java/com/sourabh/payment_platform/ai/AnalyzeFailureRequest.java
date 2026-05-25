package com.sourabh.payment_platform.ai;

import jakarta.validation.constraints.NotBlank;

public record AnalyzeFailureRequest(
        @NotBlank(message = "transactionId is required")
        String transactionId
) {
}
