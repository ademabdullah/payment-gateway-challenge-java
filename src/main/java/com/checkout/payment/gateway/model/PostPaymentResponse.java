package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class PostPaymentResponse {

  private final UUID id;
  private final PaymentStatus status;

  @JsonProperty("last_four")
  private final String lastFour;

  @JsonProperty("expiry_month")
  private final int expiryMonth;

  @JsonProperty("expiry_year")
  private final int expiryYear;

  private final String currency;
  private final int amount;

  public PostPaymentResponse(
      UUID id,
      PaymentStatus status,
      String lastFour,
      int expiryMonth,
      int expiryYear,
      String currency,
      int amount) {
    this.id = id;
    this.status = status;
    this.lastFour = lastFour;
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
    this.currency = currency;
    this.amount = amount;
  }

  public UUID getId() {
    return id;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getLastFour() {
    return lastFour;
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public int getAmount() {
    return amount;
  }
}
