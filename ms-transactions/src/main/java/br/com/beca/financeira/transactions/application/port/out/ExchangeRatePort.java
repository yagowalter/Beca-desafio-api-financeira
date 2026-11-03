package br.com.beca.financeira.transactions.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRatePort {

    ExchangeRate findByCurrencyAndDate(String currency, LocalDate date);


    record ExchangeRate(
            BigDecimal sellingRate,
            LocalDate quoteDate
    ) {

    }
}
