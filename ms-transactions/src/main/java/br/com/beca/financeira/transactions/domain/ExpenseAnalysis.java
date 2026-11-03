package br.com.beca.financeira.transactions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenseAnalysis(
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal totalSpent,
        List<CategoryTotal> byCategory,
        List<DailyTotal> byDay,
        List<MonthlyTotal> byMonth
) {
    public record CategoryTotal(String category, BigDecimal total) {
    }

    public record DailyTotal(LocalDate date, BigDecimal total) {
    }

    public record MonthlyTotal(String month, BigDecimal total) {
    }
}

