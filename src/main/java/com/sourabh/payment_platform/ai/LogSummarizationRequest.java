package com.sourabh.payment_platform.ai;

import jakarta.validation.constraints.NotBlank;

public record LogSummarizationRequest(
        @NotBlank(message = "logs are required")
        String logs
) {
}
