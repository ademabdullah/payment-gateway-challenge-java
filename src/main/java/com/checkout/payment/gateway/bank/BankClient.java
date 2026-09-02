package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.model.PostPaymentRequest;

public interface BankClient {

  boolean authorize(PostPaymentRequest paymentRequest);
}
