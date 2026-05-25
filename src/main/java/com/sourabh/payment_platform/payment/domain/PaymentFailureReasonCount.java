package com.sourabh.payment_platform.payment.domain;

public interface PaymentFailureReasonCount {

    String getFailureReason();

    long getFailureCount();
}
