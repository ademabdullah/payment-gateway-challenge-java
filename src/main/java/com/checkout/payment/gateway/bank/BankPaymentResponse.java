package com.checkout.payment.gateway.bank;

import com.fasterxml.jackson.annotation.JsonProperty;

class BankPaymentResponse {

  private Boolean authorized;

  @JsonProperty("authorization_code")
  private String authorizationCode;

  public Boolean getAuthorized() {
    return authorized;
  }

  public void setAuthorized(Boolean authorized) {
    this.authorized = authorized;
  }

  public String getAuthorizationCode() {
    return authorizationCode;
  }

  public void setAuthorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
  }
}
