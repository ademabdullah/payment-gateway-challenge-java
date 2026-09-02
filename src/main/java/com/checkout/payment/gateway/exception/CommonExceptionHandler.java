package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
  public ResponseEntity<ErrorResponse> handleRejectedRequest(Exception exception) {
    LOG.info("Payment request rejected");
    return ResponseEntity.badRequest().body(new ErrorResponse("Rejected"));
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException exception) {
    LOG.info("A payment for this paymentId could not be found");
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Payment not found"));
  }

  @ExceptionHandler(BankCommunicationException.class)
  public ResponseEntity<ErrorResponse> handleBankFailure(BankCommunicationException exception) {
    LOG.warn("Payment could not be completed because the call to the bank has failed");
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(new ErrorResponse("Bank is unavailable"));
  }
}
