package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.ValidExpiryDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@ValidExpiryDate
public class PostPaymentRequest {

  @JsonProperty("card_number")
  @NotBlank(message = "is required")
  @Size(min = 14, max = 19, message = "must contain between 14 and 19 digits")
  @Pattern(regexp = "\\d+", message = "must contain only numeric characters")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @NotNull(message = "is required")
  private Integer expiryMonth;

  @JsonProperty("expiry_year")
  @NotNull(message = "is required")
  private Integer expiryYear;

  @NotBlank(message = "is required")
  @Pattern(regexp = "GBP|USD|EUR", message = "must be one of GBP, USD or EUR")
  private String currency;

  @NotNull(message = "is required")
  @Positive(message = "must be greater than zero")
  private Integer amount;

  @NotBlank(message = "is required")
  @Pattern(regexp = "\\d{3,4}", message = "must contain 3 or 4 numeric characters")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public Integer getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(Integer expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public Integer getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(Integer expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }
}
