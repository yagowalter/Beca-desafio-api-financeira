package br.com.beca.financeira.transactions.adapters.in.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionRequestValidator.class)
public @interface ValidTransactionRequest {

    String message() default "Dados da transação inválidos";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
