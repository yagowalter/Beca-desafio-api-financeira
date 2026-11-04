package br.com.beca.financeira.transactions.adapters.in.web;

import br.com.beca.financeira.transactions.application.port.service.AccountLimitsUnavailableException;
import br.com.beca.financeira.transactions.application.port.service.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

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
}
