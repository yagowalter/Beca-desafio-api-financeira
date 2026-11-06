
package br.com.beca.financeira.transactions.adapters.in.web.dto;

import br.com.beca.financeira.transactions.domain.TransactionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CreateTransactionRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    private CreateTransactionRequest validRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setDescription("Compra no mercado");
        request.setAmount(new BigDecimal("25.00"));
        request.setType(TransactionType.PURCHASE);
        request.setCategory("Alimentação");
        return request;
    }

    @Test
    void shouldAcceptValidTransactionRequest() {
        var request = validRequest();

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        var request = validRequest();
        request.setAmount(new BigDecimal("10.999"));

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("amount")
        ));
    }

    @Test
    void shouldRejectDescriptionLongerThan150Characters() {
        var request = validRequest();
        request.setDescription("A".repeat(151));

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("description")
        ));
    }

    @Test
    void shouldRejectCategoryLongerThan255Characters() {
        var request = validRequest();
        request.setCategory("A".repeat(256));

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("category")
        ));
    }

    @Test
    void shouldRejectAmountExceedingIntegerPrecision() {
        var request = validRequest();
        request.setAmount(new BigDecimal("123456789012345678.00"));

        var violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("amount")
        ));
    }
}
