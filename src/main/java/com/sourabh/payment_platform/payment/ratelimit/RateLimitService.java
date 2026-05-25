package com.sourabh.payment_platform.payment.ratelimit;

import com.sourabh.payment_platform.shared.exception.RateLimitExceededException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.rate-limit.payment-create-limit}")
    private long paymentCreateLimit;

    @Value("${app.rate-limit.payment-create-window}")
    private Duration paymentCreateWindow;

    public void validateCreatePaymentAllowed(String userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        Long requestCount = stringRedisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1L) {
            stringRedisTemplate.expire(key, paymentCreateWindow);
        }

        if (requestCount != null && requestCount > paymentCreateLimit) {
            log.warn("Rate limit exceeded for userId: {}, requestCount: {}", userId, requestCount);
            throw new RateLimitExceededException("Rate limit exceeded. Try again later.");
        }
    }
}
