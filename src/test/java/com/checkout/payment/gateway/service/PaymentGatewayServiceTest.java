package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.bank.BankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PaymentGatewayServiceTest {

  private PaymentsRepository repository;
  private BankClient bankClient;
  private PaymentGatewayService service;
  private PostPaymentRequest paymentRequest;

  @BeforeEach
  void setUp() {
    repository = new PaymentsRepository();
    bankClient = mock(BankClient.class);
    service = new PaymentGatewayService(repository, bankClient);
    paymentRequest = request();
  }

  @Test
  void retrievesDetailsOfAStoredPaymentSuccessfully() {
    PostPaymentResponse payment = paymentResponse();
    repository.add(payment);

    assertThat(service.getPaymentById(payment.getId())).isSameAs(payment);
  }

  @Test
  void throwsAnErrorWhenPaymentDetailsAreNotStored() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> service.getPaymentById(id))
        .isInstanceOf(PaymentNotFoundException.class)
        .hasMessage("Payment not found");
  }

  @Test
  void storesOnlySafeDetailsForAuthorizedPayments() {
    when(bankClient.authorize(paymentRequest)).thenReturn(true);

    PostPaymentResponse result = service.processPayment(paymentRequest, null);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(result.getLastFour()).isEqualTo("0001");
    assertThat(repository.get(result.getId())).containsSame(result);
    verify(bankClient).authorize(paymentRequest);
  }

  @Test
  void mapsAnUnauthorizedBankResponseToDeclined() {
    when(bankClient.authorize(paymentRequest)).thenReturn(false);

    PostPaymentResponse result = service.processPayment(paymentRequest, null);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(repository.get(result.getId())).containsSame(result);
  }

  @Test
  void returnsTheOriginalPaymentWhenAnIdempotencyKeyIsReused() {
    when(bankClient.authorize(paymentRequest)).thenReturn(true);

    PostPaymentResponse firstPayment = service.processPayment(paymentRequest, "payment-key");
    PostPaymentResponse repeatedPayment = service.processPayment(paymentRequest, "payment-key");

    assertThat(repeatedPayment).isSameAs(firstPayment);
    verify(bankClient, times(1)).authorize(paymentRequest);
  }

  @Test
  void processesDifferentIdempotencyKeysAsDifferentPayments() {
    when(bankClient.authorize(paymentRequest)).thenReturn(true);

    PostPaymentResponse firstPayment = service.processPayment(paymentRequest, "first-payment-key");
    PostPaymentResponse secondPayment =
        service.processPayment(paymentRequest, "second-payment-key");

    assertThat(secondPayment.getId()).isNotEqualTo(firstPayment.getId());
    verify(bankClient, times(2)).authorize(paymentRequest);
  }

  private PostPaymentRequest request() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("12345678900001");
    request.setExpiryMonth(12);
    request.setExpiryYear(2099);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }

  private PostPaymentResponse paymentResponse() {
    return new PostPaymentResponse(
        UUID.randomUUID(), PaymentStatus.AUTHORIZED, "0001", 12, 2099, "GBP", 100);
  }
}
