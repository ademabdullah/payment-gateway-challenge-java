package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.bank.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;

  public PaymentGatewayService(
      PaymentsRepository paymentsRepository, AcquiringBankClient acquiringBankClient) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBankClient = acquiringBankClient;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.info("Request made to access payment with ID {}", id);
    return paymentsRepository
        .get(id)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest paymentRequest) {
    UUID paymentId = UUID.randomUUID();
    LOG.info("Request made to process payment with ID {}", paymentId);

    boolean authorized = acquiringBankClient.authorize(paymentRequest);
    PaymentStatus status = authorized ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
    String lastFour =
        paymentRequest.getCardNumber().substring(paymentRequest.getCardNumber().length() - 4);
    PostPaymentResponse payment =
        new PostPaymentResponse(
            paymentId,
            status,
            lastFour,
            paymentRequest.getExpiryMonth(),
            paymentRequest.getExpiryYear(),
            paymentRequest.getCurrency(),
            paymentRequest.getAmount());
    paymentsRepository.add(payment);

    LOG.info("Payment processed id={} status={}", paymentId, status.getName());
    return payment;
  }
}
