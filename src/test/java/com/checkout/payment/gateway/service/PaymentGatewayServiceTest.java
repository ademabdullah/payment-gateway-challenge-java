package com.checkout.payment.gateway.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.bank.AcquiringBankClient;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import org.junit.jupiter.api.Test;

class PaymentGatewayServiceTest {

  @Test
  void storesOnlySafeDetailsForAuthorizedPayments() {
    PaymentsRepository repository = new PaymentsRepository();
    AcquiringBankClient bankClient = mock(AcquiringBankClient.class);
    PostPaymentRequest request = request();
    when(bankClient.authorize(request)).thenReturn(true);
    PaymentGatewayService service = new PaymentGatewayService(repository, bankClient);

    PaymentResponse result = service.processPayment(request);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(result.getLastFour()).isEqualTo("0001");
    assertThat(repository.get(result.getId())).containsSame(result);
    verify(bankClient).authorize(request);
  }

  @Test
  void mapsAnUnauthorizedBankResponseToDeclined() {
    PaymentsRepository repository = new PaymentsRepository();
    AcquiringBankClient bankClient = mock(AcquiringBankClient.class);
    PostPaymentRequest request = request();
    when(bankClient.authorize(request)).thenReturn(false);
    PaymentGatewayService service = new PaymentGatewayService(repository, bankClient);

    PaymentResponse result = service.processPayment(request);

    assertThat(result.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(repository.get(result.getId())).containsSame(result);
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
}
