package com.sourabh.payment_platform.ai;

public class AiProcessingException extends RuntimeException {

    public AiProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiProcessingException(String message) {
        super(message);
    }
}
