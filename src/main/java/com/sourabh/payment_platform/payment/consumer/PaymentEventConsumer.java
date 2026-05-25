package com.sourabh.payment_platform.payment.consumer;

import com.sourabh.payment_platform.payment.api.PaymentResponse;
import com.sourabh.payment_platform.payment.cache.PaymentCacheService;
import com.sourabh.payment_platform.payment.client.BankApiResponse;
import com.sourabh.payment_platform.payment.domain.Payment;
import com.sourabh.payment_platform.payment.domain.PaymentRepository;
import com.sourabh.payment_platform.payment.domain.PaymentStatus;
import com.sourabh.payment_platform.payment.event.PaymentCreatedEvent;
import com.sourabh.payment_platform.payment.service.BankApiService;
import com.sourabh.payment_platform.shared.exception.PaymentNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final PaymentRepository paymentRepository;
    private final BankApiService bankApiService;
    private final PaymentCacheService paymentCacheService;

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.payment-created-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentCreatedEventKafkaListenerContainerFactory"
    )
    public void consumePaymentCreated(PaymentCreatedEvent event) {
        log.info("Kafka event consumed for transactionId: {}, userId: {}", event.getTransactionId(), event.getUserId());

        Payment payment = paymentRepository.findByTransactionId(event.getTransactionId())
                .orElseThrow(() -> new PaymentNotFoundException(event.getTransactionId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Skipping async processing for transactionId: {} because status is {}", payment.getTransactionId(), payment.getStatus());
            paymentCacheService.cachePayment(toResponse(payment));
            return;
        }

        log.info("Async payment processing started for transactionId: {}", payment.getTransactionId());
        BankApiResponse bankApiResponse = bankApiService.callBankWithResilience(
                payment.getTransactionId(),
                payment.getAmount(),
                payment.getUserId()
        );

        if (bankApiResponse.success()) {
            payment.markSuccess();
            log.info("Payment processing success for transactionId: {}", payment.getTransactionId());
        } else {
            payment.markFailure(bankApiResponse.failureReason());
            log.warn("Payment processing failure for transactionId: {}, reason: {}", payment.getTransactionId(), bankApiResponse.failureReason());
        }

        Payment updatedPayment = paymentRepository.save(payment);
        paymentCacheService.cachePayment(toResponse(updatedPayment));
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
}
