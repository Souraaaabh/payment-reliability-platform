package com.sourabh.payment_platform.payment.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByTransactionId(String transactionId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findAllByOrderByCreatedAtDesc();

    List<Payment> findAllByUserIdOrderByCreatedAtDesc(String userId);

    List<Payment> findAllByStatusAndUpdatedAtAfterOrderByUpdatedAtDesc(PaymentStatus status, Instant updatedAt);

    long countByStatusAndUpdatedAtAfter(PaymentStatus status, Instant updatedAt);

    long countByUpdatedAtAfter(Instant updatedAt);

    @Query("""
            select p.failureReason as failureReason, count(p) as failureCount
            from Payment p
            where p.status = com.sourabh.payment_platform.payment.domain.PaymentStatus.FAILED
              and p.updatedAt >= :from
            group by p.failureReason
            order by count(p) desc
            """)
    List<PaymentFailureReasonCount> summarizeFailureReasonsSince(@Param("from") Instant from);
}
