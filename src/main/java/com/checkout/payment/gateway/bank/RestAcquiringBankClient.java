package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.exception.BankCommunicationException;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class RestAcquiringBankClient implements AcquiringBankClient {

  private static final Logger LOG = LoggerFactory.getLogger(RestAcquiringBankClient.class);

  private final RestTemplate restTemplate;
  private final String paymentsUrl;

  public RestAcquiringBankClient(
      RestTemplate restTemplate, @Value("${bank.base-url:http://localhost:8080}") String baseUrl) {
    this.restTemplate = restTemplate;
    this.paymentsUrl = baseUrl + "/payments";
  }

  @Override
  public boolean authorize(PostPaymentRequest paymentRequest) {
    BankPaymentRequest bankRequest =
        new BankPaymentRequest(
            paymentRequest.getCardNumber(),
            String.format(
                "%02d/%d", paymentRequest.getExpiryMonth(), paymentRequest.getExpiryYear()),
            paymentRequest.getCurrency(),
            paymentRequest.getAmount(),
            paymentRequest.getCvv());

    long startedAt = System.nanoTime();
    try {
      ResponseEntity<BankPaymentResponse> response =
          restTemplate.postForEntity(paymentsUrl, bankRequest, BankPaymentResponse.class);
      BankPaymentResponse responseBody = response.getBody();
      if (responseBody == null || responseBody.getAuthorized() == null) {
        throw new BankCommunicationException("Acquiring bank returned an invalid response");
      }

      LOG.info(
          "Acquiring bank call completed status={} authorized={} duration_ms={}",
          response.getStatusCode().value(),
          responseBody.getAuthorized(),
          elapsedMilliseconds(startedAt));
      return responseBody.getAuthorized();
    } catch (BankCommunicationException exception) {
      LOG.warn(
          "Acquiring bank call returned an invalid response duration_ms={}",
          elapsedMilliseconds(startedAt));
      throw exception;
    } catch (RestClientException exception) {
      LOG.warn(
          "Acquiring bank call failed type={} duration_ms={}",
          exception.getClass().getSimpleName(),
          elapsedMilliseconds(startedAt));
      throw new BankCommunicationException("Acquiring bank request failed", exception);
    }
  }

  private long elapsedMilliseconds(long startedAt) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
  }
}
