package br.com.beca.financeira.transactions.adapters.out.http;

import br.com.beca.financeira.transactions.application.port.out.ExchangeRatePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Locale;

@Component
public class ExchangeRateAdapter implements ExchangeRatePort {

    private final RestClient restClient;
    private final String exchangeRateUrl;

    public ExchangeRateAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${brasilapi.exchange-rate-url}") String exchangeRateUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.exchangeRateUrl = exchangeRateUrl;
    }

    @Override
    public ExchangeRate findByCurrencyAndDate(String currency, LocalDate date) {
        ExchangeRateResponse response = restClient.get()
                .uri(
                        exchangeRateUrl + "/{currency}/{date}",
                        currency.toUpperCase(Locale.ROOT),
                        date
                )
                .retrieve()
                .body(ExchangeRateResponse.class);

        if (response == null
                || response.getData() == null
                || response.getCotacoes() == null
                || response.getCotacoes().isEmpty()) {
            throw new IllegalStateException(
                    "A BrasilAPI não retornou cotação para " + currency + " na data " + date
            );
        }

        ExchangeRateResponse.QuoteResponse quote = response.getCotacoes().stream()
                .filter(item -> "FECHAMENTO PTAX".equalsIgnoreCase(item.getBulletinType()))
                .findFirst()
                .orElse(response.getCotacoes().getLast());

        if (quote.getSellingRate() == null) {
            throw new IllegalStateException(
                    "A brasilAPI retornou cotação de venda vazia"
            );
        }

        return new ExchangeRate(
                quote.getSellingRate(),
                response.getData()
        );

    }

}

