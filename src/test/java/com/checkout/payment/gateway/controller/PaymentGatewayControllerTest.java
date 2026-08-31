package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.bank.AcquiringBankClient;
import com.jayway.jsonpath.JsonPath;
import java.time.YearMonth;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired private MockMvc mvc;

  @MockBean private AcquiringBankClient acquiringBankClient;

  @Test
  void processesAndRetrievesAnAuthorizedPayment() throws Exception {
    when(acquiringBankClient.authorize(any())).thenReturn(true);

    MvcResult postResult =
        mvc.perform(
                post("/payment").contentType(MediaType.APPLICATION_JSON).content(validRequest(100)))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", Matchers.matchesPattern(".*/payment/.+")))
            .andExpect(jsonPath("$.status").value("Authorized"))
            .andExpect(jsonPath("$.last_four").value("8877"))
            .andExpect(jsonPath("$.card_number").doesNotExist())
            .andExpect(jsonPath("$.cvv").doesNotExist())
            .andReturn();

    String id = JsonPath.read(postResult.getResponse().getContentAsString(), "$.id");
    mvc.perform(get("/payment/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.last_four").value("8877"))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void rejectsZeroAndNegativeAmountsBeforeCallingTheBank() throws Exception {
    mvc.perform(post("/payment").contentType(MediaType.APPLICATION_JSON).content(validRequest(0)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected"));

    mvc.perform(post("/payment").contentType(MediaType.APPLICATION_JSON).content(validRequest(-1)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected"));

    verifyNoInteractions(acquiringBankClient);
  }

  @Test
  void rejectsInvalidCardFieldsBeforeCallingTheBank() throws Exception {
    int futureYear = YearMonth.now().plusYears(1).getYear();
    String request =
        """
        {
          "card_number": "123x",
          "expiry_month": 13,
          "expiry_year": %d,
          "currency": "AUD",
          "amount": 100,
          "cvv": "1x"
        }
        """
            .formatted(futureYear);

    mvc.perform(post("/payment").contentType(MediaType.APPLICATION_JSON).content(request))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected"));

    verifyNoInteractions(acquiringBankClient);
  }

  @Test
  void rejectsAnExpiredCardBeforeCallingTheBank() throws Exception {
    mvc.perform(
            post("/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request(100, YearMonth.now())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Rejected"));

    verifyNoInteractions(acquiringBankClient);
  }

  @Test
  void returnsUsefulErrorsForUnknownAndMalformedIds() throws Exception {
    mvc.perform(get("/payment/5c6f8caf-4163-4f4b-ac0d-14bb13cb29e3"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Page not found"));

    mvc.perform(get("/payment/not-a-uuid")).andExpect(status().isBadRequest());
  }

  private String validRequest(int amount) {
    return request(amount, YearMonth.now().plusYears(1));
  }

  private String request(int amount, YearMonth expiry) {
    return """
        {
          "card_number": "%s",
          "expiry_month": %d,
          "expiry_year": %d,
          "currency": "GBP",
          "amount": %d,
          "cvv": "123"
        }
        """
        .formatted("2222405343248877", expiry.getMonthValue(), expiry.getYear(), amount);
  }
}
