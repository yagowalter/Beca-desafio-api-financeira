package br.com.beca.financeira.transactions.application.port.in;

import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionUseCase {

    Transaction create(
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type
    );

    List<Transaction> findAll();

    Transaction findById(Long id);

    Transaction update(
            Long id,
            String description,
            BigDecimal amount,
            TransactionType type
    );

    void delete(Long id);
}
