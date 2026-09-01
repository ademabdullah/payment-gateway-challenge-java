package com.checkout.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.time.YearMonth;
import org.junit.jupiter.api.Tag;
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

  @Container
  private static final GenericContainer<?> BANK_SIMULATOR =
      new GenericContainer<>(DockerImageName.parse("bbyars/mountebank:2.8.1"))
          .withExposedPorts(2525, 8080)
          .withFileSystemBind(
              Paths.get("imposters").toAbsolutePath().toString(), "/imposters", BindMode.READ_ONLY)
          .withCommand("--configfile", "/imposters/bank_simulator.ejs", "--allowInjection");

  @DynamicPropertySource
  static void bankProperties(DynamicPropertyRegistry registry) {
    registry.add("bank.base-url", () -> "http://localhost:" + BANK_SIMULATOR.getMappedPort(8080));
  }

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
        restTemplate.getForEntity(url("/payment/" + id), String.class);
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
        .isEqualTo("Bank is unavailable");
  }

  @Test
  void rejectsInvalidAmountsBeforeCallingTheBank() throws Exception {
    ResponseEntity<String> response = postPayment("2222405343248877", 0);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(objectMapper.readTree(response.getBody()).get("message").asText())
        .isEqualTo("Rejected");
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
        url("/payment"), new HttpEntity<>(request, headers), String.class);
  }

  private String url(String path) {
    return "http://localhost:" + port + path;
  }
}
