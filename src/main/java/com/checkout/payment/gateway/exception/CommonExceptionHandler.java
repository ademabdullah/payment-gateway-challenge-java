package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, List<String>> errors =
        exception.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.groupingBy(
                    error -> snakeCase(error.getField()),
                    TreeMap::new,
                    Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
    errors.values().forEach(messages -> messages.sort(String::compareTo));
    LOG.info("Payment rejected validation_fields={}", errors.keySet());
    return ResponseEntity.badRequest().body(ErrorResponse.rejected(errors));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleMalformedRequest(
      HttpMessageNotReadableException exception) {
    LOG.info("Payment rejected because the request body was malformed");
    return ResponseEntity.badRequest()
        .body(ErrorResponse.rejected(Map.of("request", List.of("must be valid JSON"))));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMalformedPath(
      MethodArgumentTypeMismatchException exception) {
    LOG.info(
        "Request rejected because a path parameter was malformed parameter={}",
        exception.getName());
    return ResponseEntity.badRequest()
        .body(ErrorResponse.message("Payment id must be a valid UUID"));
  }

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.message("Payment not found"));
  }

  @ExceptionHandler(BankCommunicationException.class)
  public ResponseEntity<ErrorResponse> handleBankFailure(BankCommunicationException exception) {
    LOG.warn("Payment could not be completed because the acquiring bank call failed");
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ErrorResponse.message("Acquiring bank unavailable"));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
    LOG.error("Unexpected payment gateway failure", exception);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ErrorResponse.message("Internal server error"));
  }

  private String snakeCase(String value) {
    return value.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
  }
}
