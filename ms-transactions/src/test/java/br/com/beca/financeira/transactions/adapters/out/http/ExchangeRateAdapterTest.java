
package br.com.beca.financeira.transactions.adapters.out.http;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ExchangeRateAdapterTest {

    @Test
    void shouldSelectClosingPtaxRateAndUppercaseCurrency() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        ExchangeRateAdapter adapter = new ExchangeRateAdapter(
                builder,
                "https://brasilapi.com.br/api/cambio/v1/cotacao"
        );

        LocalDate date = LocalDate.of(2026, 11, 5);

        server.expect(requestTo(
                        "https://brasilapi.com.br/api/cambio/v1/cotacao/USD/2026-11-05"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        {
                            "moeda": "USD",
                            "data": "2026-11-05",
                            "cotacoes": [
                                {
                                    "cotacao_venda": 5.25,
                                    "tipo_boletim": "FECHAMENTO PTAX",
                                    "data_hora_cotacao": "2026-11-05 13:00:00"
                                },
                                {
                                    "cotacao_venda": 5.30,
                                    "tipo_boletim": "ABERTURA",
                                    "data_hora_cotacao": "2026-11-05 09:00:00"
                                }
                            ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var result = adapter.findByCurrencyAndDate("usd", date);

        assertNotNull(result);
        assertEquals(0, new BigDecimal("5.25")
                .compareTo(result.sellingRate()));
        assertEquals(date, result.quoteDate());


        server.verify();
    }


    @Test
    void shouldUseLastQuoteWhenClosingPtaxIsUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        ExchangeRateAdapter adapter = new ExchangeRateAdapter(
                builder,
                "https://brasilapi.com.br/api/cambio/v1/cotacao"
        );

        LocalDate date = LocalDate.of(2026, 11, 5);

        server.expect(requestTo(
                        "https://brasilapi.com.br/api/cambio/v1/cotacao/USD/2026-11-05"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        {
                            "moeda": "USD",
                            "data": "2026-11-05",
                            "cotacoes": [
                                {
                                    "cotacao_venda": 5.20,
                                    "tipo_boletim": "ABERTURA"
                                },
                                {
                                    "cotacao_venda": 5.30,
                                    "tipo_boletim": "INTERMEDIARIO"
                                }
                            ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        var result = adapter.findByCurrencyAndDate("USD", date);

        assertEquals(0, new BigDecimal("5.30")
                .compareTo(result.sellingRate()));
        assertEquals(date, result.quoteDate());

        server.verify();
    }

    @Test
    void shouldRejectEmptyQuotesFromBrasilApi() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        ExchangeRateAdapter adapter = new ExchangeRateAdapter(
                builder,
                "https://brasilapi.com.br/api/cambio/v1/cotacao"
        );

        LocalDate date = LocalDate.of(2026, 11, 5);

        server.expect(requestTo(
                        "https://brasilapi.com.br/api/cambio/v1/cotacao/USD/2026-11-05"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        {
                            "moeda": "USD",
                            "data": "2026-11-05",
                            "cotacoes": []
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.findByCurrencyAndDate("USD", date)
        );

        assertTrue(exception.getMessage().contains("não retornou cotação"));

        server.verify();
    }

    @Test
    void shouldRejectQuoteWithoutSellingRate() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        ExchangeRateAdapter adapter = new ExchangeRateAdapter(
                builder,
                "https://brasilapi.com.br/api/cambio/v1/cotacao"
        );

        LocalDate date = LocalDate.of(2026, 11, 5);

        server.expect(requestTo(
                        "https://brasilapi.com.br/api/cambio/v1/cotacao/USD/2026-11-05"
                ))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        """
                        {
                            "moeda": "USD",
                            "data": "2026-11-05",
                            "cotacoes": [
                                {
                                    "cotacao_venda": null,
                                    "tipo_boletim": "FECHAMENTO PTAX"
                                }
                            ]
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> adapter.findByCurrencyAndDate("USD", date)
        );

        assertEquals(
                "A brasilAPI retornou cotação de venda vazia",
                exception.getMessage()
        );

        server.verify();
    }


}
