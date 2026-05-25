package com.sourabh.payment_platform.shared.api;

import com.sourabh.payment_platform.ai.AiProcessingException;
import com.sourabh.payment_platform.exception.BankApiException;
import com.sourabh.payment_platform.shared.exception.DuplicatePaymentException;
import com.sourabh.payment_platform.shared.exception.PaymentNotFoundException;
import com.sourabh.payment_platform.shared.exception.RateLimitExceededException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> details = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toDetail)
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        List<String> details = exception.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    @ExceptionHandler(DuplicatePaymentException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicatePayment(DuplicatePaymentException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), List.of());
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentNotFound(PaymentNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), List.of());
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceeded(RateLimitExceededException exception) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, exception.getMessage(), List.of());
    }

    @ExceptionHandler(BankApiException.class)
    public ResponseEntity<ApiErrorResponse> handleBankApiException(BankApiException exception) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), List.of());
    }

    @ExceptionHandler(AiProcessingException.class)
    public ResponseEntity<ApiErrorResponse> handleAiProcessingException(AiProcessingException exception) {
        return buildResponse(HttpStatus.BAD_GATEWAY, exception.getMessage(), List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found: " + exception.getResourcePath(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", List.of(exception.getClass().getSimpleName()));
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, List<String> details) {
        ApiErrorResponse response = new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );

        return ResponseEntity.status(status).body(response);
    }

    private String toDetail(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
