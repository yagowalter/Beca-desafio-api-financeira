package br.com.beca.financeira.transactions.application.port.in;

import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionUseCase {

    Transaction create(
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            Long destinationUserId
    );

    List<Transaction> findAllByUserId(Long userId);

    List<Transaction> findAllByUserIdAndPeriod(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );

    Transaction findByIdAndUserId(Long id, Long userId);
}