package br.com.beca.financeira.transactions.application.port.in;

import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;

import java.time.LocalDate;

public interface ExpenseAnalysisUseCase {

    ExpenseAnalysis analyzeExpenses(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}