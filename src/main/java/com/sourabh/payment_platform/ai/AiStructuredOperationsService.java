package com.sourabh.payment_platform.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiStructuredOperationsService {

    private final ChatClient chatClient;

    public AiStructuredOperationsService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public <T> T executeStructuredOperation(
            String operationName,
            String systemPrompt,
            String userPrompt,
            Class<T> responseType
    ) {
        long startNanos = System.nanoTime();
        log.info("AI request started for operation: {}", operationName);
        log.info("AI prompt generated for operation: {}, systemPromptChars: {}, userPromptChars: {}",
                operationName, systemPrompt.length(), userPrompt.length());

        try {
            T response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .entity(responseType);

            if (response == null) {
                throw new AiProcessingException("AI returned an empty structured response for operation: " + operationName);
            }

            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.info("AI response generated for operation: {}, latencyMs: {}", operationName, latencyMs);
            return response;
        } catch (AiProcessingException exception) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("AI failure for operation: {}, latencyMs: {}, cause: {}", operationName, latencyMs, exception.getMessage());
            throw exception;
        } catch (Exception exception) {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;
            log.error("AI failure for operation: {}, latencyMs: {}, cause: {}", operationName, latencyMs, exception.toString());
            throw new AiProcessingException("AI processing failed for operation: " + operationName, exception);
        }
    }
}
