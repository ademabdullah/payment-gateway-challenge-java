package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

@Component
public class ExpiryDateValidator
    implements ConstraintValidator<ValidExpiryDate, PostPaymentRequest> {

  private final Clock clock;

  public ExpiryDateValidator(Clock clock) {
    this.clock = clock;
  }

  @Override
  public boolean isValid(PostPaymentRequest request, ConstraintValidatorContext context) {
    if (request == null || request.getExpiryMonth() == null || request.getExpiryYear() == null) {
      return true;
    }

    try {
      boolean valid =
          YearMonth.of(request.getExpiryYear(), request.getExpiryMonth())
              .isAfter(YearMonth.now(clock));
      if (!valid) {
        addExpiryViolation(context);
      }
      return valid;
    } catch (DateTimeException exception) {
      addExpiryViolation(context);
      return false;
    }
  }

  private void addExpiryViolation(ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    context
        .buildConstraintViolationWithTemplate("must form a valid future expiry date")
        .addPropertyNode("expiryYear")
        .addConstraintViolation();
  }
}
