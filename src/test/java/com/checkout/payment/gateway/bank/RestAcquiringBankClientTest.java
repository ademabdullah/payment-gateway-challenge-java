package com.checkout.payment.gateway.bank;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServiceUnavailable;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class RestAcquiringBankClientTest {

  private MockRestServiceServer server;
  private RestAcquiringBankClient client;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    client = new RestAcquiringBankClient(restTemplate, "http://bank.test");
  }

  @Test
  void sendsTheExpectedPayloadAndMapsAuthorization() {
    server
        .expect(requestTo("http://bank.test/payments"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(
            content()
                .json(
                    """
            {
              "card_number": "2222405343248877",
              "expiry_date": "04/2099",
              "currency": "GBP",
              "amount": 100,
              "cvv": "123"
            }
            """))
        .andRespond(
            withSuccess(
                "{\"authorized\":true,\"authorization_code\":\"abc\"}",
                MediaType.APPLICATION_JSON));

    assertThat(client.authorize(request())).isTrue();
    server.verify();
  }

  @Test
  void mapsBankServiceFailuresToTheGatewayException() {
    server.expect(requestTo("http://bank.test/payments")).andRespond(withServiceUnavailable());

    assertThatThrownBy(() -> client.authorize(request()))
        .isInstanceOf(BankCommunicationException.class);
    server.verify();
  }

  @Test
  void rejectsMalformedSuccessfulResponses() {
    server
        .expect(requestTo("http://bank.test/payments"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.authorize(request()))
        .isInstanceOf(BankCommunicationException.class);
  }

  private PostPaymentRequest request() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2099);
    request.setCurrency("GBP");
    request.setAmount(100);
    request.setCvv("123");
    return request;
  }
}
