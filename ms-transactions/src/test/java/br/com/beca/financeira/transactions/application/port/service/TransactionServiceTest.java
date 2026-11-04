package br.com.beca.financeira.transactions.application.port.service;


import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.financeira.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Test
    void shouldNotSaveTransactionWhenAccountIsNotFound() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        LocalDate today = LocalDate.now();

        when(exchangeRatePort.findByCurrencyAndDate("USD", today))
                .thenReturn(new ExchangeRatePort.ExchangeRate(
                        new BigDecimal("5.00"), today
                ));
        when(exchangeRatePort.findByCurrencyAndDate("EUR", today))
                .thenReturn(new ExchangeRatePort.ExchangeRate(
                        new BigDecimal("5.50"), today
                ));

        when(accountLimitsPort.findByUserId(7L))
                .thenThrow(new AccountNotFoundException("Conta não encontrada"));

        assertThrows(
                AccountNotFoundException.class,
                () -> service.create(
                        7L,
                        "Compra teste",
                        new BigDecimal("50.00"),
                        TransactionType.PURCHASE,
                        "Alimentação",
                        null
                )
        );

        verify(transactionRepositoryPort, never()).save(any());
    }

    @Test
    void shouldNotSaveTransactionWhenAccountServiceIsUnavailable() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        LocalDate today = LocalDate.now();

        when(exchangeRatePort.findByCurrencyAndDate("USD", today))
                .thenReturn(new ExchangeRatePort.ExchangeRate(
                        new BigDecimal("5.00"), today
                ));
        when(exchangeRatePort.findByCurrencyAndDate("EUR", today))
                .thenReturn(new ExchangeRatePort.ExchangeRate(
                        new BigDecimal("5.50"), today
                ));

        when(accountLimitsPort.findByUserId(7L))
                .thenThrow(new AccountLimitsUnavailableException("MockAPI indisponível"));

        assertThrows(
                AccountLimitsUnavailableException.class,
                () -> service.create(
                        7L,
                        "Compra teste",
                        new BigDecimal("50.00"),
                        TransactionType.PURCHASE,
                        "Alimentação",
                        null
                )
        );
        verify(transactionRepositoryPort, never()).save(any());
    }

    @Test
    void shouldSaveTransactionWithoutExchangeRatesWhenNoQuoteIsAvailable() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        when(exchangeRatePort.findByCurrencyAndDate(anyString(), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("Cotação não encontrada"));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Depósito teste",
                new BigDecimal("50.00"),
                TransactionType.DEPOSIT,
                "Salário",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertFalse(savedTransaction.getValue().isExchangeRatesAvailable());
    }

    @Test
    void shouldUsePreviousDateWhenTodaysQuoteIsUnavailable() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        LocalDate today = LocalDate.now();
        LocalDate yesterDay = today.minusDays(1);

        when(exchangeRatePort.findByCurrencyAndDate("USD", today))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        HttpHeaders.EMPTY,
                        "NO_TODAY_DATE".getBytes(StandardCharsets.UTF_8),
                        StandardCharsets.UTF_8
                ));
        when(exchangeRatePort.findByCurrencyAndDate("USD", yesterDay))
                .thenReturn(new ExchangeRatePort.ExchangeRate(
                        new BigDecimal("5.00"), yesterDay
                ));
        when(exchangeRatePort.findByCurrencyAndDate("EUR", yesterDay))
                .thenReturn(new ExchangeRatePort.ExchangeRate(
                        new BigDecimal("5.50"), yesterDay
                ));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Depósito teste",
                new BigDecimal("50.00"),
                TransactionType.DEPOSIT,
                "Salário",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertTrue(savedTransaction.getValue().isExchangeRatesAvailable());
        assertEquals(yesterDay, savedTransaction.getValue().getUsdQuoteDate());
        assertEquals(yesterDay, savedTransaction.getValue().getEurQuoteDate());
    }

    @Test
    void shouldSaveTransactionWithoutExchangeRatesWhenCommunicationFails() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        when(exchangeRatePort.findByCurrencyAndDate("USD", LocalDate.now()))
                .thenThrow(new ResourceAccessException("Falha de comunicação"));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Depósito teste",
                new BigDecimal("50.00"),
                TransactionType.DEPOSIT,
                "Salário",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertFalse(savedTransaction.getValue().isExchangeRatesAvailable());
        verify(exchangeRatePort, times(1))
                .findByCurrencyAndDate("USD", LocalDate.now());
    }
}
