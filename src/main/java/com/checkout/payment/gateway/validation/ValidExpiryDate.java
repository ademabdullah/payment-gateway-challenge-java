package com.checkout.payment.gateway.validation;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Constraint(validatedBy = ExpiryDateValidator.class)
@Documented
public @interface ValidExpiryDate {

  String message() default "must be a valid future expiry date";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
