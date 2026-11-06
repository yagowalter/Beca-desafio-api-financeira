package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.in.ExpenseAnalysisUseCase;
import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionDomainException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ExpenseAnalysisService implements ExpenseAnalysisUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public ExpenseAnalysisService(
            TransactionRepositoryPort transactionRepositoryPort
    ) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    @Override
    public ExpenseAnalysis analyzeExpenses(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Period period = resolvePeriod(userId, startDate, endDate);

        List<Transaction> transactions =
                transactionRepositoryPort.findApprovedOutflowsByUserIdAndPeriod(
                        userId,
                        period.start().atStartOfDay(),
                        period.end().plusDays(1).atStartOfDay()
                );

        return buildAnalysis(period, transactions);
    }

    private Period resolvePeriod(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        if (userId == null || userId <= 0) {
            throw new TransactionDomainException("Usuário inválido");
        }

        if (startDate == null && endDate == null) {
            YearMonth currentMonth = YearMonth.now();
            return new Period(currentMonth.atDay(1), currentMonth.atEndOfMonth());
        }

        if (startDate == null || endDate == null) {
            throw new TransactionDomainException(
                    "Informe a data inicial e a data final"
            );
        }

        if (endDate.isBefore(startDate)) {
            throw new TransactionDomainException(
                    "A data final não pode ser anterior à data inicial"
            );
        }

        return new Period(startDate, endDate);
    }

    private ExpenseAnalysis buildAnalysis(
            Period period,
            List<Transaction> transactions
    ) {
        Map<String, BigDecimal> categoryTotals = new TreeMap<>();
        Map<LocalDate, BigDecimal> dailyTotals = new TreeMap<>();
        Map<YearMonth, BigDecimal> monthlyTotals = new TreeMap<>();
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Transaction transaction : transactions) {
            BigDecimal amount = transaction.getAmount();
            LocalDate date = transaction.getOccurredAt().toLocalDate();

            String category = transaction.getCategory();
            if (category == null || category.isBlank()) {
                category = "sem categoria";
            }

            totalSpent = totalSpent.add(amount);
            categoryTotals.merge(category, amount, BigDecimal::add);
            dailyTotals.merge(date, amount, BigDecimal::add);
            monthlyTotals.merge(YearMonth.from(date), amount, BigDecimal::add);
        }

        return new ExpenseAnalysis(
                period.start(),
                period.end(),
                totalSpent,
                categoryTotals.entrySet().stream()
                        .map(entry -> new ExpenseAnalysis.CategoryTotal(
                                entry.getKey(), entry.getValue()
                        ))
                        .toList(),
                dailyTotals.entrySet().stream()
                        .map(entry -> new ExpenseAnalysis.DailyTotal(
                                entry.getKey(), entry.getValue()
                        ))
                        .toList(),
                monthlyTotals.entrySet().stream()
                        .map(entry -> new ExpenseAnalysis.MonthlyTotal(
                                entry.getKey().toString(), entry.getValue()
                        ))
                        .toList()
        );
    }

    private record Period(LocalDate start, LocalDate end) {
    }
}