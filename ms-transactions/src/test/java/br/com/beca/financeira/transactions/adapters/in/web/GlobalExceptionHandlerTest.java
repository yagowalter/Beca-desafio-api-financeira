package br.com.beca.financeira.transactions.adapters.in.web;

import br.com.beca.financeira.transactions.application.port.service.AccountLimitsUnavailableException;
import br.com.beca.financeira.transactions.application.port.service.AccountNotFoundException;
import br.com.beca.financeira.transactions.application.port.service.TransactionNotFoundException;
import br.com.beca.financeira.transactions.domain.TransactionDomainException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldReturnNotFoundProblemWhenAccountIsMissing(){
        var problem = handler.handleAccountNotFound(
                new AccountNotFoundException("Conta não encontrada")
        );

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Conta não encontrada" , problem.getDetail());
    }

    @Test
    void shouldReturnServiceUnavailableProblemWhenAccountServiceFails(){
        var problem = handler.handleAccountLimitsUnavailable(
                new AccountLimitsUnavailableException(
                        "Não foi possível consultar saldo e limtes da conta"
                )
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), problem.getStatus());
        assertEquals(
                "Não foi possível consultar saldo e limtes da conta",
                problem.getDetail()
        );
    }

    @Test
    void shouldReturnNotFoundWhenTransactionDoesNotExist() {
        ProblemDetail response = handler.handleNotFound(
                new TransactionNotFoundException("Transação não encontrada")
        );

        assertEquals(404, response.getStatus());
        assertEquals("Transação não encontrada", response.getDetail());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalid() {
        var problem = handler.handleValidation(
                new HttpMessageNotReadableException("JSON inválido", null)
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals(
                "Corpo da requisição inválido ou com valores desconhecidos",
                problem.getDetail()
        );
    }

    @Test
    void shouldReturnBadRequestWhenDomainValidationFails() {
        var problem = handler.handleDomainValidation(
                new TransactionDomainException("Valor deve ser maior do que zero")
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals(
                "Valor deve ser maior do que zero",
                problem.getDetail()
        );
    }

    @Test
    void shouldReturnBadRequestWithFieldErrorsWhenValidationFails() {
        var bindingResult = new BeanPropertyBindingResult(
                new Object(),
                "createTransactionRequest"
        );

        bindingResult.addError(
                new FieldError(
                        "createTransactionRequest",
                        "description",
                        "Descrição é obrigatória"
                )
        );

        var exception = new MethodArgumentNotValidException(
                null,
                bindingResult
        );

        var problem = handler.handleValidation(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals(
                "Um ou mais campos estão inválidos",
                problem.getDetail()
        );

        assertEquals(
                List.of(Map.of(
                        "campo", "description",
                        "message", "Descrição é obrigatória"
                )),
                problem.getProperties().get("erros")
        );
    }

    @Test
    void shouldReturnFixedMessageWhenGenericIllegalArgumentOccurs() {
        var problem = handler.handleIllegalArgument(
                new IllegalArgumentException("mensagem interna")
        );

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("Requisição inválida", problem.getDetail());
    }

}
