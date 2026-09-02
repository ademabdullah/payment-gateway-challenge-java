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

class AcquiringBankClientTest {

  private static final String BANK_BASE_URL = "http://bank.com";
  private static final String BANK_PAYMENTS_URL = BANK_BASE_URL + "/payments";

  private MockRestServiceServer server;
  private AcquiringBankClient client;

  @BeforeEach
  void setUp() {
    RestTemplate restTemplate = new RestTemplate();
    server = MockRestServiceServer.bindTo(restTemplate).build();
    client = new AcquiringBankClient(restTemplate, BANK_BASE_URL);
  }

  @Test
  void WhenAValidPayloadIsSentAValidResponseIsReturned() {
    PostPaymentRequest paymentRequest = validPaymentRequest();

    server
        .expect(requestTo(BANK_PAYMENTS_URL))
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

    boolean authorized = client.authorize(paymentRequest);

    assertThat(authorized).isTrue();
    server.verify();
  }

  @Test
  void WhenARequestToTheAcquiringBankFailsThisIsMappedToAGatewayException() {
    PostPaymentRequest paymentRequest = validPaymentRequest();

    server.expect(requestTo(BANK_PAYMENTS_URL)).andRespond(withServiceUnavailable());

    assertThatThrownBy(() -> client.authorize(paymentRequest))
        .isInstanceOf(BankCommunicationException.class)
        .hasMessage("Acquiring bank request failed");

    server.verify();
  }

  @Test
  void rejectsMalformedSuccessfulResponses() {
    PostPaymentRequest paymentRequest = validPaymentRequest();

    server
        .expect(requestTo(BANK_PAYMENTS_URL))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.authorize(paymentRequest))
        .isInstanceOf(BankCommunicationException.class)
        .hasMessage("Acquiring bank returned an invalid response");
  }

  private PostPaymentRequest validPaymentRequest() {
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
