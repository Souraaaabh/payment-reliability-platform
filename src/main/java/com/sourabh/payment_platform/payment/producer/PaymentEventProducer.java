package com.sourabh.payment_platform.payment.producer;

import com.sourabh.payment_platform.payment.event.PaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventProducer {

    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;

    @Value("${app.kafka.payment-created-topic}")
    private String paymentCreatedTopic;

    public void publishPaymentCreated(PaymentCreatedEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
            return;
        }

        send(event);
    }

    private void send(PaymentCreatedEvent event) {
        kafkaTemplate.send(paymentCreatedTopic, event.getTransactionId(), event)
                .whenComplete((result, throwable) -> {
                    if (throwable == null) {
                        log.info("Kafka event published for transactionId: {}, topic: {}", event.getTransactionId(), paymentCreatedTopic);
                        return;
                    }

                    log.error("Kafka event publish failed for transactionId: {}, cause: {}", event.getTransactionId(), throwable.toString());
                });
    }
}
