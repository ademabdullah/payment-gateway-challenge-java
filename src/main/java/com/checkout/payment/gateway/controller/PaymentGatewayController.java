package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PaymentResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/payments")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @PostMapping
  public ResponseEntity<PaymentResponse> processPayment(
      @Valid @RequestBody PostPaymentRequest paymentRequest) {
    PaymentResponse payment = paymentGatewayService.processPayment(paymentRequest);
    URI location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(payment.getId())
            .toUri();
    return ResponseEntity.created(location).body(payment);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable UUID id) {
    return ResponseEntity.ok(paymentGatewayService.getPaymentById(id));
  }
}
