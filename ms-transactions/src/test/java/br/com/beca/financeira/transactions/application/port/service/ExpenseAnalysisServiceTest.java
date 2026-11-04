package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ExpenseAnalysisServiceTest {

    private TransactionRepositoryPort transactionRepositoryPort;
    private ExpenseAnalysisService service;

    @BeforeEach
    void setUp() {
        transactionRepositoryPort = mock(TransactionRepositoryPort.class);
        service = new ExpenseAnalysisService(transactionRepositoryPort);
    }

    @Test
    void shouldAggregateExpensesByCategoryDayAndMonth() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        Transaction food = new Transaction(
                1L, 1L, "Mercado", new BigDecimal("20.00"),
                TransactionType.PURCHASE, LocalDateTime.of(2026, 11, 4, 10, 0),
                "Alimentação", null, null, null,
                null, null, null, null, false
        );
        Transaction transport = new Transaction(
                2L, 1L, "Ônibus", new BigDecimal("8.00"),
                TransactionType.PURCHASE, LocalDateTime.of(2026, 11, 4, 12, 0),
                "Transporte", null, null, null,
                null, null, null, null, false
        );

        when(transactionRepositoryPort.findApprovedOutflowsByUserIdAndPeriod(
                eq(7L), any(), any()
        )).thenReturn(List.of(food, transport));

        ExpenseAnalysis result = service.analyzeExpenses(7L, start, end);

        assertEquals(2, result.byCategory().size());
        assertEquals(1, result.byDay().size());
        assertEquals(1, result.byMonth().size());
    }

    @Test
    void shouldRejectInvalidUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyzeExpenses(0L, null, null)
        );

        verifyNoInteractions(transactionRepositoryPort);
    }

    @Test
    void shouldRejectRangeWithOnlyOneDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyzeExpenses(
                        7L, LocalDate.of(2026, 11, 1), null
                )
        );

        verifyNoInteractions(transactionRepositoryPort);
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.analyzeExpenses(
                        7L,
                        LocalDate.of(2026, 11, 10),
                        LocalDate.of(2026, 11, 1)
                )
        );

        verifyNoInteractions(transactionRepositoryPort);
    }

    @Test
    void shouldUseCurrentMonthWhenDatesAreOmitted() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());

        when(transactionRepositoryPort.findApprovedOutflowsByUserIdAndPeriod(
                eq(7L),
                eq(monthStart.atStartOfDay()),
                eq(monthEnd.plusDays(1).atStartOfDay())
        )).thenReturn(List.of());

        ExpenseAnalysis result = service.analyzeExpenses(7L, null, null);

        assertEquals(monthStart, result.periodStart());
        assertEquals(monthEnd, result.periodEnd());
        assertEquals(BigDecimal.ZERO, result.totalSpent());
    }
}