package br.com.beca.financeira.transactions.application.port.out;

import br.com.beca.financeira.transactions.domain.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    List<Transaction> findAll();

    Optional<Transaction> findById(Long id);

    BigDecimal sumApprovedOutflowsByUserIdAndPeriod(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Transaction> findApprovedOutflowsByUserIdAndPeriod(
            Long userId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

}