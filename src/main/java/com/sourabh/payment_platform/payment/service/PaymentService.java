package com.sourabh.payment_platform.payment.service;

import com.sourabh.payment_platform.payment.api.CreatePaymentRequest;
import com.sourabh.payment_platform.payment.api.PaymentResponse;
import java.util.List;

public interface PaymentService {

    PaymentResponse createPayment(CreatePaymentRequest request);

    PaymentResponse getPaymentByTransactionId(String transactionId);

    List<PaymentResponse> listPayments(String userId);
}
