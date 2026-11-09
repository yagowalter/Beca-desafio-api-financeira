package br.com.beca.financeira.transactions.application.port.service;


import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.financeira.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionDomainException;
import br.com.beca.financeira.transactions.domain.TransactionStatus;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private void mockSuccessfulSave(TransactionRepositoryPort repository) {
        when(repository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldNotSaveTransactionWhenAccountIsNotFound() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        mockSuccessfulSave(transactionRepositoryPort);
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
        mockSuccessfulSave(transactionRepositoryPort);
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
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        when(exchangeRatePort.findByCurrencyAndDate(anyString(), any(LocalDate.class)))
                .thenThrow(new IllegalStateException("Cotação não encontrada"));

        when(accountLimitsPort.findByUserId(7L))
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
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

        assertFalse(savedTransaction.getValue().isExchangeRatesAvailable());
    }

    @Test
    void shouldUsePreviousDateWhenTodaysQuoteIsUnavailable() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        LocalDate today = LocalDate.now();
        LocalDate yesterDay = today.minusDays(1);

        when(accountLimitsPort.findByUserId(7L))
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                ));

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
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

        when(accountLimitsPort.findByUserId(7L))
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                ));

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

    @Test
    void shouldRejectTransactionWhenBalanceIsInsufficient() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

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
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("100.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("200.00")
                ));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Compra teste",
                new BigDecimal("150.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertEquals(TransactionStatus.REJECTED, savedTransaction.getValue().getStatus());
        assertEquals(
                "Saldo insuficiente",
                savedTransaction.getValue().getRejectionReason()
        );
        verify(transactionRepositoryPort, never())
                .sumApprovedOutflowsByUserIdAndPeriod(any(), any(), any());
    }

    @Test
    void shouldRejectTransactionWhenPerTransactionLimitIsExceeded() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

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
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("500.00"),
                        new BigDecimal("800.00"),
                        new BigDecimal("100.00")
                ));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Compra teste",
                new BigDecimal("150.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertEquals(TransactionStatus.REJECTED, savedTransaction.getValue().getStatus());
        assertEquals(
                "Valor acima do limite por transação",
                savedTransaction.getValue().getRejectionReason()
        );
        verify(transactionRepositoryPort, never())
                .sumApprovedOutflowsByUserIdAndPeriod(any(), any(), any());
    }

    @Test
    void shouldRejectTransactionWhenDailyLimitIsExceeded() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

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
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("500.00"),
                        new BigDecimal("200.00"),
                        new BigDecimal("300.00")
                ));
        when(transactionRepositoryPort.sumApprovedOutflowsByUserIdAndPeriod(
                eq(7L), any(), any()
        )).thenReturn(new BigDecimal("150.00"));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Compra teste",
                new BigDecimal("100.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertEquals(TransactionStatus.REJECTED, savedTransaction.getValue().getStatus());
        assertEquals(
                "Limite diário excedido",
                savedTransaction.getValue().getRejectionReason()
        );
    }

    @Test
    void shouldApproveTransactionWhenBalanceAndLimitsAreSufficient() {
        TransactionRepositoryPort transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        mockSuccessfulSave(transactionRepositoryPort);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

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
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("500.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("200.00")
                ));
        when(transactionRepositoryPort.sumApprovedOutflowsByUserIdAndPeriod(
                eq(7L), any(), any()
        )).thenReturn(new BigDecimal("50.00"));

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Compra teste",
                new BigDecimal("100.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        ArgumentCaptor<Transaction> savedTransaction =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(savedTransaction.capture());

        assertEquals(TransactionStatus.APPROVED, savedTransaction.getValue().getStatus());
        assertNull(savedTransaction.getValue().getRejectionReason());
    }

    @Test
    void shouldFindTransactionsByUserAndInclusiveDatePeriod() {
        TransactionRepositoryPort repository = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimits = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRates = mock(ExchangeRatePort.class);
        TransactionService service = new TransactionService(repository, accountLimits, exchangeRates);

        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        when(repository.findAllByUserIdAndPeriod(
                7L,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());

        assertTrue(service.findAllByUserIdAndPeriod(7L, start, end).isEmpty());

        verify(repository).findAllByUserIdAndPeriod(
                7L,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );
    }

    @Test
    void shouldRejectInvalidUserOrPeriodBeforeRepositoryCall() {
        TransactionRepositoryPort repository = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimits = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRates = mock(ExchangeRatePort.class);
        TransactionService service = new TransactionService(repository, accountLimits, exchangeRates);

        LocalDate start = LocalDate.of(2026, 11, 10);
        LocalDate end = LocalDate.of(2026, 11, 20);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAllByUserIdAndPeriod(0L, start, end)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAllByUserIdAndPeriod(7L, null, end)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAllByUserIdAndPeriod(7L, start, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> service.findAllByUserIdAndPeriod(7L, end, start)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldFindTransactionsForUserWithoutFilters() {
        TransactionRepositoryPort repository = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimits = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRates = mock(ExchangeRatePort.class);
        TransactionService service = new TransactionService(repository, accountLimits, exchangeRates);

        when(repository.findAllByUserId(7L)).thenReturn(List.of());

        assertTrue(service.findAllByUserIdAndFilters(7L, null, null, null).isEmpty());

        verify(repository).findAllByUserId(7L);
    }

    @Test
    void shouldFindTransactionsByTypeWithoutPeriod() {
        TransactionRepositoryPort repository = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimits = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRates = mock(ExchangeRatePort.class);
        TransactionService service =
                new TransactionService(repository, accountLimits, exchangeRates);

        when(repository.findAllByUserIdAndType(
                7L, TransactionType.PURCHASE
        )).thenReturn(List.of());

        assertTrue(service.findAllByUserIdAndFilters(
                7L, null, null, TransactionType.PURCHASE
        ).isEmpty());

        verify(repository).findAllByUserIdAndType(
                7L, TransactionType.PURCHASE
        );
    }

    @Test
    void shouldFindTransactionsByTypeAndPeriod() {
        TransactionRepositoryPort repository = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimits = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRates = mock(ExchangeRatePort.class);
        TransactionService service =
                new TransactionService(repository, accountLimits, exchangeRates);

        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        when(repository.findAllByUserIdAndTypeAndPeriod(
                7L,
                TransactionType.PURCHASE,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        )).thenReturn(List.of());

        assertTrue(service.findAllByUserIdAndFilters(
                7L, start, end, TransactionType.PURCHASE
        ).isEmpty());

        verify(repository).findAllByUserIdAndTypeAndPeriod(
                7L,
                TransactionType.PURCHASE,
                start.atStartOfDay(),
                end.plusDays(1).atStartOfDay()
        );
    }

    @Test
    void shouldRejectInvalidFiltersBeforeRepositoryCall() {
        TransactionRepositoryPort repository = mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimits = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRates = mock(ExchangeRatePort.class);
        TransactionService service = new TransactionService(repository, accountLimits, exchangeRates);

        LocalDate start = LocalDate.of(2026, 11, 10);
        LocalDate end = LocalDate.of(2026, 11, 20);

        assertThrows(
                TransactionDomainException.class,
                () -> service.findAllByUserIdAndFilters(0L, null, null, null)
        );
        assertThrows(
                TransactionDomainException.class,
                () -> service.findAllByUserIdAndFilters(7L, start, null, null)
        );
        assertThrows(
                TransactionDomainException.class,
                () -> service.findAllByUserIdAndFilters(7L, null, end, null)
        );
        assertThrows(
                TransactionDomainException.class,
                () -> service.findAllByUserIdAndFilters(7L, end, start, null)
        );

        verifyNoInteractions(repository);
    }

    @Test
    void shouldSaveIncomingCreditForRecipientWhenTransferIsApproved() {
        TransactionRepositoryPort transactionRepositoryPort =
                mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

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
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("1000.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                ));
        when(accountLimitsPort.findByUserId(8L))
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("50.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                ));

        when(transactionRepositoryPort.sumApprovedOutflowsByUserIdAndPeriod(
                eq(7L), any(), any()
        )).thenReturn(BigDecimal.ZERO);

        mockSuccessfulSave(transactionRepositoryPort);

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Transferência para usuário 8",
                new BigDecimal("100.00"),
                TransactionType.TRANSFER,
                "Transferência",
                8L
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort, times(2)).save(captor.capture());

        List<Transaction> savedTransactions = captor.getAllValues();
        Transaction outgoing = savedTransactions.get(0);
        Transaction incomingCredit = savedTransactions.get(1);

        assertEquals(TransactionType.TRANSFER, outgoing.getType());
        assertEquals(7L, outgoing.getUserId());
        assertEquals(TransactionStatus.APPROVED, outgoing.getStatus());

        assertEquals(TransactionType.DEPOSIT, incomingCredit.getType());
        assertEquals(8L, incomingCredit.getUserId());
        assertEquals(new BigDecimal("100.00"), incomingCredit.getAmount());
        assertEquals(TransactionStatus.APPROVED, incomingCredit.getStatus());

        verify(accountLimitsPort).updateBalance(
                7L, new BigDecimal("900.00")
        );
        verify(accountLimitsPort).updateBalance(
                8L, new BigDecimal("150.00")
        );
    }

    @Test
    void shouldNotCreateIncomingCreditWhenTransferIsRejected() {
        TransactionRepositoryPort transactionRepositoryPort =
                mock(TransactionRepositoryPort.class);
        AccountLimitsPort accountLimitsPort = mock(AccountLimitsPort.class);
        ExchangeRatePort exchangeRatePort = mock(ExchangeRatePort.class);

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
                .thenReturn(new AccountLimitsPort.AccountLimits(
                        new BigDecimal("50.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00")
                ));

        mockSuccessfulSave(transactionRepositoryPort);

        TransactionService service = new TransactionService(
                transactionRepositoryPort,
                accountLimitsPort,
                exchangeRatePort
        );

        service.create(
                7L,
                "Transferência para usuário 8",
                new BigDecimal("100.00"),
                TransactionType.TRANSFER,
                "Transferência",
                8L
        );

        ArgumentCaptor<Transaction> captor =
                ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepositoryPort).save(captor.capture());

        assertEquals(TransactionStatus.REJECTED, captor.getValue().getStatus());
        assertEquals(TransactionType.TRANSFER, captor.getValue().getType());

        verify(accountLimitsPort, never()).updateBalance(eq(8L), any());
    }

}
