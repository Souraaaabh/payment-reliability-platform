package com.sourabh.payment_platform.ai;

import jakarta.validation.constraints.NotBlank;

public record OperationalQueryRequest(
        @NotBlank(message = "question is required")
        String question
) {
}
