package com.sourabh.payment_platform.config;

import com.sourabh.payment_platform.payment.event.PaymentCreatedEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.config.TopicBuilder;

@EnableKafka
@Configuration
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, PaymentCreatedEvent> paymentCreatedEventProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = new HashMap<>(kafkaProperties.buildProducerProperties());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        return new DefaultKafkaProducerFactory<>(
                properties,
                new StringSerializer(),
                new JsonSerializer<>()
        );
    }

    @Bean
    public ConsumerFactory<String, PaymentCreatedEvent> paymentCreatedEventConsumerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> properties = new HashMap<>(kafkaProperties.buildConsumerProperties());
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        JsonDeserializer<PaymentCreatedEvent> jsonDeserializer = new JsonDeserializer<>(PaymentCreatedEvent.class, false);
        jsonDeserializer.addTrustedPackages("com.sourabh.payment_platform.payment.event");

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public KafkaTemplate<String, PaymentCreatedEvent> paymentCreatedEventKafkaTemplate(
            ProducerFactory<String, PaymentCreatedEvent> paymentCreatedEventProducerFactory
    ) {
        return new KafkaTemplate<>(paymentCreatedEventProducerFactory);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCreatedEvent> paymentCreatedEventKafkaListenerContainerFactory(
            ConsumerFactory<String, PaymentCreatedEvent> paymentCreatedEventConsumerFactory,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup
    ) {
        ConcurrentKafkaListenerContainerFactory<String, PaymentCreatedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentCreatedEventConsumerFactory);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    @ConditionalOnProperty(value = "app.kafka.auto-create-topic", havingValue = "true", matchIfMissing = true)
    public NewTopic paymentCreatedEventsTopic(@Value("${app.kafka.payment-created-topic}") String topicName) {
        return TopicBuilder.name(topicName)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
