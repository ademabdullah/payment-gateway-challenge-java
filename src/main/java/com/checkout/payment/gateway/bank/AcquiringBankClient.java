package com.checkout.payment.gateway.bank;

import com.checkout.payment.gateway.model.PostPaymentRequest;

public interface AcquiringBankClient {

  boolean authorize(PostPaymentRequest paymentRequest);
}
