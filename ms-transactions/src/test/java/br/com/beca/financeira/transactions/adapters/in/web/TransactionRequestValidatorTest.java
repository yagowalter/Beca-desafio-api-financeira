package br.com.beca.financeira.transactions.adapters.in.web;

import br.com.beca.financeira.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.financeira.transactions.domain.TransactionType;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRequestValidatorTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void configureValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectPurchaseWithoutCategory() {
        CreateTransactionRequest request = validRequest();
        request.setType(TransactionType.PURCHASE);

        assertViolation(
                request,
                "category",
                "Categoria é obrigatória para compras"
        );
    }

    @Test
    void shouldRejectTransferWithoutDestinationUser() {
        CreateTransactionRequest request = validRequest();
        request.setType(TransactionType.TRANSFER);
        request.setCategory("Transferência");

        assertViolation(
                request,
                "destinationUserId",
                "Usuário de destino é obrigatório para transferências"
        );
    }

    @Test
    void shouldRejectTransferWithNonPositiveDestinationUserId() {
        CreateTransactionRequest request = validRequest();
        request.setType(TransactionType.TRANSFER);
        request.setCategory("Transferência");
        request.setDestinationUserId(0L);

        assertViolation(
                request,
                "destinationUserId",
                "Usuário de destino é obrigatório para transferências"
        );
    }

    @Test
    void shouldAcceptPurchaseWithCategory() {
        CreateTransactionRequest request = validRequest();
        request.setType(TransactionType.PURCHASE);
        request.setCategory("Alimentação");

        assertTrue(validator.validate(request).isEmpty());
    }

    private CreateTransactionRequest validRequest() {
        CreateTransactionRequest request = new CreateTransactionRequest();
        request.setDescription("Transação de teste");
        request.setAmount(new BigDecimal("10.00"));
        request.setType(TransactionType.DEPOSIT);
        return request;
    }

    private void assertViolation(
            CreateTransactionRequest request,
            String expectedField,
            String expectedMessage
    ) {
        boolean violationFound = validator.validate(request)
                .stream()
                .anyMatch(violation ->
                        violation.getPropertyPath().toString().equals(expectedField)
                                && violation.getMessage().equals(expectedMessage)
                );

        assertFalse(
                validator.validate(request).isEmpty() && !violationFound,
                "Esperava erro no campo " + expectedField + ": " + expectedMessage
        );
        assertTrue(violationFound);
    }

    @Test
    void shouldAcceptTransferWithoutCategory() {
        CreateTransactionRequest request = validRequest();
        request.setType(TransactionType.TRANSFER);
        request.setDestinationUserId(54L);

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAcceptTransferWithCategoryAndDestination() {
        CreateTransactionRequest request = validRequest();
        request.setType(TransactionType.TRANSFER);
        request.setCategory("Transferência");
        request.setDestinationUserId(54L);

        assertTrue(validator.validate(request).isEmpty());
    }
}