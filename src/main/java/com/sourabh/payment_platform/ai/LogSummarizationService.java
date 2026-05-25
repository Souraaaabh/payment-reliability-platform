package com.sourabh.payment_platform.ai;

import org.springframework.stereotype.Service;

@Service
public class LogSummarizationService {

    private final AiPromptFactory aiPromptFactory;
    private final AiStructuredOperationsService aiStructuredOperationsService;
    private final AiResponseSanitizer aiResponseSanitizer;

    public LogSummarizationService(
            AiPromptFactory aiPromptFactory,
            AiStructuredOperationsService aiStructuredOperationsService,
            AiResponseSanitizer aiResponseSanitizer
    ) {
        this.aiPromptFactory = aiPromptFactory;
        this.aiStructuredOperationsService = aiStructuredOperationsService;
        this.aiResponseSanitizer = aiResponseSanitizer;
    }

    public LogSummaryResponse summarizeLogs(String logs) {
        LogSummaryResponse response = aiStructuredOperationsService.executeStructuredOperation(
                "summarize-logs",
                aiPromptFactory.logSummarizationSystemPrompt(),
                aiPromptFactory.logSummarizationUserPrompt(logs),
                LogSummaryResponse.class
        );

        return aiResponseSanitizer.sanitizeLogSummary(response);
    }
}
