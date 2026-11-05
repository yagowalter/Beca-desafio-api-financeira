package br.com.beca.financeira.transactions.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TransactionTest {

    @Test
    void shouldConvertAmountToUsdAndEurWhenExchangeRatesAreAvailable() {
        Transaction transaction = new Transaction(
                7L,
                "Depósito teste",
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT
        );
        LocalDate quoteDate = LocalDate.of(2026, 11, 5);

        transaction.registerExchangeRates(
                new BigDecimal("5.00"),
                quoteDate,
                new BigDecimal("4.00"),
                quoteDate
        );

        assertTrue(transaction.isExchangeRatesAvailable());
        assertEquals(new BigDecimal("20.00"), transaction.getAmountUsd());
        assertEquals(new BigDecimal("25.00"), transaction.getAmountEur());
    }

    @Test
    void shouldReturnNullConvertedAmountsWhenExchangeRatesAreUnavailable() {
        Transaction transaction = new Transaction(
                7L,
                "Depósito teste",
                new BigDecimal("100.00"),
                TransactionType.DEPOSIT
        );

        assertFalse(transaction.isExchangeRatesAvailable());
        assertNull(transaction.getAmountUsd());
        assertNull(transaction.getAmountEur());
    }

    @Test
    void shouldSetRejectedStatusAndReason() {
        Transaction transaction = new Transaction(
                7L,
                "Compra teste",
                new BigDecimal("50.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        transaction.reject("Saldo insuficiente");

        assertEquals(TransactionStatus.REJECTED, transaction.getStatus());
        assertEquals("Saldo insuficiente", transaction.getRejectionReason());
    }

    @Test
    void shouldRejectBlankRejectionReason() {
        Transaction transaction = new Transaction(
                7L,
                "Compra teste",
                new BigDecimal("50.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> transaction.reject(" ")
        );
    }

    @Test
    void shouldNotFinalizeTransactionMoreThanOnce() {
        Transaction transaction = new Transaction(
                7L,
                "Depósito teste",
                new BigDecimal("50.00"),
                TransactionType.DEPOSIT
        );

        transaction.approve();

        assertThrows(IllegalStateException.class, transaction::approve);
        assertThrows(
                IllegalStateException.class,
                () -> transaction.reject("Motivo")
        );
    }

    @Test
    void shouldAllowTransactionWhenAmountsMatchLimitsExactly() {
        Transaction transaction = new Transaction(
                7L,
                "Compra teste",
                new BigDecimal("100.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        assertNull(transaction.validateBalanceAndTransactionLimit(
                new BigDecimal("100.00"),
                new BigDecimal("100.00")
        ));
        assertNull(transaction.validateDailyLimit(
                new BigDecimal("400.00"),
                new BigDecimal("500.00")
        ));
    }
}