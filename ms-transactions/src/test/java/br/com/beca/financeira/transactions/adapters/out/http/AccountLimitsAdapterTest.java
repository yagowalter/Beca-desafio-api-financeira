
package br.com.beca.financeira.transactions.adapters.out.http;

import br.com.beca.financeira.transactions.application.port.service.AccountLimitsUnavailableException;
import br.com.beca.financeira.transactions.application.port.service.AccountNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AccountLimitsAdapterTest {

    @Test
    void shouldFetchAccountLimitsSuccessfully() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        AccountLimitsAdapter adapter = new AccountLimitsAdapter(
                builder,
                "http://localhost:8080/accounts"
        );

        server.expect(requestTo(
                        "http://localhost:8080/accounts?userId=7"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        [
                            {
                                "userId": 7,
                                "balance": 1500.00,
                                "dailyLimit": 2000.00,
                                "perTransactionLimit": 500.00
                            }
                        ]
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var result = adapter.findByUserId(7L);

        assertEquals(0, new BigDecimal("1500.00")
                .compareTo(result.balance()));
        assertEquals(0, new BigDecimal("2000.00")
                .compareTo(result.dailyLimit()));
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(result.perTransactionLimit()));

        server.verify();
    }

    @Test
    void shouldThrowAccountNotFoundWhenResponseIsEmpty() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        AccountLimitsAdapter adapter = new AccountLimitsAdapter(
                builder,
                "http://localhost:8080/accounts"
        );

        server.expect(requestTo(
                        "http://localhost:8080/accounts?userId=7"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "[]",
                        MediaType.APPLICATION_JSON
                ));

        AccountNotFoundException exception = assertThrows(
                AccountNotFoundException.class,
                () -> adapter.findByUserId(7L)
        );

        assertEquals(
                "Conta não encontrada para o usuário 7",
                exception.getMessage()
        );

        server.verify();
    }

    @Test
    void shouldThrowAccountNotFoundWhenResponseBodyIsNull() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        AccountLimitsAdapter adapter = new AccountLimitsAdapter(
                builder,
                "http://localhost:8080/accounts"
        );

        server.expect(requestTo(
                        "http://localhost:8080/accounts?userId=7"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess());

        assertThrows(
                AccountNotFoundException.class,
                () -> adapter.findByUserId(7L)
        );

        server.verify();
    }

    @Test
    void shouldThrowUnavailableWhenAccountsServiceFails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        AccountLimitsAdapter adapter = new AccountLimitsAdapter(
                builder,
                "http://localhost:8080/accounts"
        );

        server.expect(requestTo(
                        "http://localhost:8080/accounts?userId=7"
                ))
                .andExpect(method(GET))
                .andRespond(withServerError());

        AccountLimitsUnavailableException exception = assertThrows(
                AccountLimitsUnavailableException.class,
                () -> adapter.findByUserId(7L)
        );

        assertEquals(
                "Não foi possível consultar saldo e limites da conta",
                exception.getMessage()
        );

        server.verify();
    }
}
