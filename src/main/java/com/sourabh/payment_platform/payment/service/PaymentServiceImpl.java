package com.sourabh.payment_platform.payment.service;

import com.sourabh.payment_platform.payment.api.CreatePaymentRequest;
import com.sourabh.payment_platform.payment.api.PaymentResponse;
import com.sourabh.payment_platform.payment.cache.PaymentCacheService;
import com.sourabh.payment_platform.payment.domain.Payment;
import com.sourabh.payment_platform.payment.domain.PaymentRepository;
import com.sourabh.payment_platform.payment.domain.PaymentStatus;
import com.sourabh.payment_platform.payment.event.PaymentCreatedEvent;
import com.sourabh.payment_platform.payment.producer.PaymentEventProducer;
import com.sourabh.payment_platform.payment.ratelimit.RateLimitService;
import com.sourabh.payment_platform.shared.exception.PaymentNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final RateLimitService rateLimitService;
    private final PaymentCacheService paymentCacheService;

    @Override
    @Transactional
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        String userId = request.userId().trim();
        String idempotencyKey = request.idempotencyKey().trim();

        rateLimitService.validateCreatePaymentAllowed(userId);

        return paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(existingPayment -> {
                    PaymentResponse response = toResponse(existingPayment);
                    paymentCacheService.cachePayment(response);
                    return response;
                })
                .orElseGet(() -> createNewPayment(request.amount(), userId, idempotencyKey));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByTransactionId(String transactionId) {
        return paymentCacheService.getPayment(transactionId)
                .orElseGet(() -> paymentRepository.findByTransactionId(transactionId)
                        .map(payment -> {
                            PaymentResponse response = toResponse(payment);
                            paymentCacheService.cachePayment(response);
                            return response;
                        })
                        .orElseThrow(() -> new PaymentNotFoundException(transactionId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPayments(String userId) {
        List<Payment> payments = hasText(userId)
                ? paymentRepository.findAllByUserIdOrderByCreatedAtDesc(userId.trim())
                : paymentRepository.findAllByOrderByCreatedAtDesc();

        return payments.stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse createNewPayment(java.math.BigDecimal amount, String userId, String idempotencyKey) {
        Payment payment = Payment.builder()
                .transactionId(generateTransactionId())
                .userId(userId)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();
        payment.markProcessingStarted();

        Payment savedPayment = paymentRepository.save(payment);
        PaymentResponse response = toResponse(savedPayment);
        paymentCacheService.cachePayment(response);

        log.info("Payment processing started for transactionId: {}", savedPayment.getTransactionId());
        paymentEventProducer.publishPaymentCreated(toEvent(savedPayment));

        return response;
    }

    private PaymentCreatedEvent toEvent(Payment payment) {
        return PaymentCreatedEvent.builder()
                .transactionId(payment.getTransactionId())
                .userId(payment.getUserId())
                .amount(payment.getAmount())
                .idempotencyKey(payment.getIdempotencyKey())
                .createdAt(payment.getCreatedAt() == null ? Instant.now() : payment.getCreatedAt())
                .build();
    }

    private String generateTransactionId() {
        return "txn_" + UUID.randomUUID().toString().replace("-", "");
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getTransactionId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getIdempotencyKey(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
