package com.checkout.payment.gateway.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SensitiveModelTest {

  @Test
  void sensitiveModelsDoNotOverrideToString() throws ClassNotFoundException {
    Class<?> bankRequest = Class.forName("com.checkout.payment.gateway.bank.BankPaymentRequest");

    assertThat(declaresToString(PostPaymentRequest.class)).isFalse();
    assertThat(declaresToString(bankRequest)).isFalse();
  }

  private boolean declaresToString(Class<?> type) {
    return Arrays.stream(type.getDeclaredMethods())
        .anyMatch(method -> method.getName().equals("toString") && method.getParameterCount() == 0);
  }
}
