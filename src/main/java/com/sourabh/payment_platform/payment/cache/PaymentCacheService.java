package com.sourabh.payment_platform.payment.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sourabh.payment_platform.payment.api.PaymentResponse;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCacheService {

    private static final String PAYMENT_CACHE_KEY_PREFIX = "payment:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.cache.payment-ttl}")
    private Duration paymentTtl;

    public Optional<PaymentResponse> getPayment(String transactionId) {
        String key = buildKey(transactionId);
        String cachedValue = stringRedisTemplate.opsForValue().get(key);

        if (cachedValue == null) {
            log.info("Redis cache miss for transactionId: {}", transactionId);
            return Optional.empty();
        }

        log.info("Redis cache hit for transactionId: {}", transactionId);
        return Optional.of(deserialize(cachedValue, transactionId));
    }

    public void cachePayment(PaymentResponse paymentResponse) {
        String key = buildKey(paymentResponse.transactionId());
        String value = serialize(paymentResponse);
        stringRedisTemplate.opsForValue().set(key, value, paymentTtl);
    }

    private String buildKey(String transactionId) {
        return PAYMENT_CACHE_KEY_PREFIX + transactionId;
    }

    private String serialize(PaymentResponse paymentResponse) {
        try {
            return objectMapper.writeValueAsString(paymentResponse);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize payment response for transactionId: "
                    + paymentResponse.transactionId(), exception);
        }
    }

    private PaymentResponse deserialize(String cachedValue, String transactionId) {
        try {
            return objectMapper.readValue(cachedValue, PaymentResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize cached payment for transactionId: "
                    + transactionId, exception);
        }
    }
}
