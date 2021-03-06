package com.googlecode.easyec.validator.prop.annotation;

import com.googlecode.easyec.validator.prop.impl.EmailPropertyValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(FIELD)
@Retention(RUNTIME)
@Validator(EmailPropertyValidator.class)
public @interface EmailValidator {

    String message() default "common.field.invalid_email";

    boolean localized() default true;
}
