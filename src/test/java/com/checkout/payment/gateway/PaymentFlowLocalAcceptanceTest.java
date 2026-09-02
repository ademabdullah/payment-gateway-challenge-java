package com.checkout.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.file.Paths;
import java.time.YearMonth;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Tag("localAcceptance")
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaymentFlowLocalAcceptanceTest {

  private static final String AUTHORIZED_CARD = "2222405343248877";
  private static final String DECLINED_CARD = "2222405343248878";
  private static final String BANK_ERROR_CARD = "2222405343248870";
  private static final int VALID_AMOUNT = 100;
  private static final int BANK_PORT = 8080;

  @Container
  private static final GenericContainer<?> BANK_SIMULATOR =
      new GenericContainer<>(DockerImageName.parse("bbyars/mountebank:2.8.1"))
          .withExposedPorts(2525, BANK_PORT)
          .withFileSystemBind(
              Paths.get("imposters").toAbsolutePath().toString(), "/imposters", BindMode.READ_ONLY)
          .withCommand("--configfile", "/imposters/bank_simulator.ejs", "--allowInjection");

  @DynamicPropertySource
  static void bankProperties(DynamicPropertyRegistry registry) {
    registry.add(
        "bank.base-url",
        () ->
            "http://%s:%d"
                .formatted(BANK_SIMULATOR.getHost(), BANK_SIMULATOR.getMappedPort(BANK_PORT)));
  }

  @Autowired private TestRestTemplate restTemplate;

  @Test
  void authorizesAndRetrievesAPaymentThroughTheBankSimulator() {
    ResponseEntity<JsonNode> created = postPayment(AUTHORIZED_CARD, VALID_AMOUNT);
    JsonNode createdPayment = bodyOf(created);

    assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(createdPayment.get("status").asText()).isEqualTo("Authorized");
    assertThat(createdPayment.get("last_four").asText()).isEqualTo("8877");
    assertThat(createdPayment.has("card_number")).isFalse();
    assertThat(createdPayment.has("cvv")).isFalse();

    String paymentId = createdPayment.get("id").asText();
    ResponseEntity<JsonNode> retrieved =
        restTemplate.getForEntity("/payment/{id}", JsonNode.class, paymentId);
    JsonNode retrievedPayment = bodyOf(retrieved);

    assertThat(retrieved.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(retrievedPayment.get("id").asText()).isEqualTo(paymentId);
  }

  @Test
  void declinesAPaymentThroughTheBankSimulator() {
    ResponseEntity<JsonNode> response = postPayment(DECLINED_CARD, VALID_AMOUNT);
    JsonNode payment = bodyOf(response);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(payment.get("status").asText()).isEqualTo("Declined");
  }

  @Test
  void returnsBadGatewayWhenTheBankReturnsServiceUnavailable() {
    ResponseEntity<JsonNode> response = postPayment(BANK_ERROR_CARD, VALID_AMOUNT);
    JsonNode error = bodyOf(response);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(error.get("message").asText()).isEqualTo("Bank is unavailable");
  }

  @Test
  void rejectsInvalidPaymentAmount() {
    ResponseEntity<JsonNode> response = postPayment(AUTHORIZED_CARD, 0);
    JsonNode error = bodyOf(response);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(error.get("message").asText()).isEqualTo("Rejected");
  }

  @Test
  void returnsTheOriginalPaymentForARepeatedIdempotencyKey() {
    String idempotencyKey = "acceptance-payment-key";

    ResponseEntity<JsonNode> firstResponse =
        postPayment(AUTHORIZED_CARD, VALID_AMOUNT, idempotencyKey);
    ResponseEntity<JsonNode> repeatedResponse =
        postPayment(AUTHORIZED_CARD, VALID_AMOUNT, idempotencyKey);

    String firstPaymentId = bodyOf(firstResponse).get("id").asText();
    String repeatedPaymentId = bodyOf(repeatedResponse).get("id").asText();
    assertThat(repeatedPaymentId).isEqualTo(firstPaymentId);
  }

  private ResponseEntity<JsonNode> postPayment(String cardNumber, int amount) {
    return postPayment(cardNumber, amount, null);
  }

  private ResponseEntity<JsonNode> postPayment(
      String cardNumber, int amount, String idempotencyKey) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    if (idempotencyKey != null) {
      headers.set("Idempotency-Key", idempotencyKey);
    }

    PostPaymentRequest request = validPaymentRequest(cardNumber, amount);
    return restTemplate.postForEntity(
        "/payment", new HttpEntity<>(request, headers), JsonNode.class);
  }

  private PostPaymentRequest validPaymentRequest(String cardNumber, int amount) {
    YearMonth expiry = YearMonth.now().plusYears(1);
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber(cardNumber);
    request.setExpiryMonth(expiry.getMonthValue());
    request.setExpiryYear(expiry.getYear());
    request.setCurrency("GBP");
    request.setAmount(amount);
    request.setCvv("123");
    return request;
  }

  private JsonNode bodyOf(ResponseEntity<JsonNode> response) {
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }
}
