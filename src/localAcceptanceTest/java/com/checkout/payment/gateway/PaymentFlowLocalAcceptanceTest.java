package com.checkout.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowLocalAcceptanceTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  @Test
  void authorizesAndRetrievesAPaymentThroughTheBankSimulator() throws Exception {
    ResponseEntity<String> created = postPayment("2222405343248877", 100);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    JsonNode createdBody = objectMapper.readTree(created.getBody());
    assertThat(createdBody.get("status").asText()).isEqualTo("Authorized");
    assertThat(createdBody.get("last_four").asText()).isEqualTo("8877");
    assertThat(createdBody.has("card_number")).isFalse();
    assertThat(createdBody.has("cvv")).isFalse();

    String id = createdBody.get("id").asText();
    ResponseEntity<String> retrieved =
        restTemplate.getForEntity(url("/payments/" + id), String.class);
    assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(objectMapper.readTree(retrieved.getBody()).get("id").asText()).isEqualTo(id);
  }

  @Test
  void declinesAPaymentThroughTheBankSimulator() throws Exception {
    ResponseEntity<String> response = postPayment("2222405343248878", 100);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(objectMapper.readTree(response.getBody()).get("status").asText())
        .isEqualTo("Declined");
  }

  @Test
  void returnsBadGatewayWhenTheBankSimulatorIsUnavailable() throws Exception {
    ResponseEntity<String> response = postPayment("2222405343248870", 100);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(objectMapper.readTree(response.getBody()).get("message").asText())
        .isEqualTo("Acquiring bank unavailable");
  }

  @Test
  void rejectsInvalidAmountsBeforeCallingTheBank() throws Exception {
    ResponseEntity<String> response = postPayment("2222405343248877", 0);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    JsonNode body = objectMapper.readTree(response.getBody());
    assertThat(body.get("status").asText()).isEqualTo("Rejected");
    assertThat(body.at("/errors/amount/0").asText()).isEqualTo("must be greater than zero");
  }

  private ResponseEntity<String> postPayment(String cardNumber, int amount) {
    YearMonth expiry = YearMonth.now().plusYears(1);
    String request =
        """
        {
          "card_number": "%s",
          "expiry_month": %d,
          "expiry_year": %d,
          "currency": "GBP",
          "amount": %d,
          "cvv": "123"
        }
        """
            .formatted(cardNumber, expiry.getMonthValue(), expiry.getYear(), amount);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return restTemplate.postForEntity(
        url("/payments"), new HttpEntity<>(request, headers), String.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
