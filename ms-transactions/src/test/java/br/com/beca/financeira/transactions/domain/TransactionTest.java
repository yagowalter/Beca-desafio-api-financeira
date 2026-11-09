package br.com.beca.financeira.transactions.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @Test
    void shouldNotAllowTransferToOwnUser() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Transaction(
                        7L,
                        "Transferência teste",
                        new BigDecimal("100.00"),
                        TransactionType.TRANSFER,
                        "Transferência",
                        7L
                )
        );
    }

    @Test
    void shouldAllowReconstructionOfLegacySelfTransfer() {
        Transaction transaction = new Transaction(
                1L,
                7L,
                "Transferência antiga",
                new BigDecimal("100.00"),
                TransactionType.TRANSFER,
                LocalDateTime.of(2025, 1, 15, 10, 0),
                null,
                7L,
                TransactionStatus.APPROVED,
                null,
                null,
                null,
                null,
                null,
                false
        );

        assertEquals(7L, transaction.getUserId());
        assertEquals(7L, transaction.getDestinationUserId());
    }

    @Test
    void shouldAllowTransferToAnotherUser() {
        Transaction transaction = new Transaction(
                7L,
                "Transferência teste",
                new BigDecimal("100.00"),
                TransactionType.TRANSFER,
                "Transferência",
                8L
        );

        assertEquals(7L, transaction.getUserId());
        assertEquals(8L, transaction.getDestinationUserId());
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(null, "Depósito", new BigDecimal("10.00"),
                        TransactionType.DEPOSIT));
    }

    @Test
    void shouldRejectNonPositiveUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(0L, "Depósito", new BigDecimal("10.00"),
                        TransactionType.DEPOSIT));
    }

    @Test
    void shouldRejectNullOrBlankDescription() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(7L, null, new BigDecimal("10.00"),
                        TransactionType.DEPOSIT));

        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(7L, " ", new BigDecimal("10.00"),
                        TransactionType.DEPOSIT));
    }

    @Test
    void shouldRejectNullOrNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(7L, "Depósito", null, TransactionType.DEPOSIT));

        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(7L, "Depósito", BigDecimal.ZERO,
                        TransactionType.DEPOSIT));

        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(7L, "Depósito", new BigDecimal("-1.00"),
                        TransactionType.DEPOSIT));
    }

    @Test
    void shouldRejectNullTransactionType() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(7L, "Depósito", new BigDecimal("10.00"), null));
    }

    @Test
    void shouldRejectPurchaseWithoutCategory() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        7L,
                        "Compra teste",
                        new BigDecimal("50.00"),
                        TransactionType.PURCHASE,
                        null,
                        null
                ));

        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        7L,
                        "Compra teste",
                        new BigDecimal("50.00"),
                        TransactionType.PURCHASE,
                        " ",
                        null
                ));
    }

    @Test
    void shouldRejectTransferWithoutDestination() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        7L,
                        "Transferência teste",
                        new BigDecimal("50.00"),
                        TransactionType.TRANSFER,
                        "Transferência",
                        null
                ));
    }

    @Test
    void shouldRejectTransferWithNonPositiveDestination() {
        assertThrows(IllegalArgumentException.class, () ->
                new Transaction(
                        7L,
                        "Transferência teste",
                        new BigDecimal("50.00"),
                        TransactionType.TRANSFER,
                        "Transferência",
                        0L
                ));
    }

    @Test
    void shouldRejectTransferWithoutCategory() {
        assertThrows(TransactionDomainException.class, () ->
                new Transaction(
                        7L,
                        "Transferência teste",
                        new BigDecimal("50.00"),
                        TransactionType.TRANSFER,
                        null,
                        8L
                )
        );

        assertThrows(TransactionDomainException.class, () ->
                new Transaction(
                        7L,
                        "Transferência teste",
                        new BigDecimal("50.00"),
                        TransactionType.TRANSFER,
                        " ",
                        8L
                )
        );
    }
}