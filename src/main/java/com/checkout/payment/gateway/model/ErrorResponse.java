package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ErrorResponse {

  private final PaymentStatus status;
  private final String message;
  private final Map<String, List<String>> errors;

  private ErrorResponse(PaymentStatus status, String message, Map<String, List<String>> errors) {
    this.status = status;
    this.message = message;
    this.errors = errors;
  }

  public static ErrorResponse rejected(Map<String, List<String>> errors) {
    return new ErrorResponse(PaymentStatus.REJECTED, null, errors);
  }

  public static ErrorResponse message(String message) {
    return new ErrorResponse(null, message, null);
  }

  public PaymentStatus getStatus() {
    return status;
  }

  public String getMessage() {
    return message;
  }

  public Map<String, List<String>> getErrors() {
    return errors;
  }
}
