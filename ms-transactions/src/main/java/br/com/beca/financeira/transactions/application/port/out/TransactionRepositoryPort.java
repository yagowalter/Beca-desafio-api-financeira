package br.com.beca.financeira.transactions.application.port.out;

import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    List<Transaction> findAllByUserIdAndType(
            Long userId,
            TransactionType type
    );

    List<Transaction> findAllByUserIdAndTypeAndPeriod(
            Long userId,
            TransactionType type,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    List<Transaction> findAll();

    List<Transaction> findAllByUserId(Long userId);

    Optional<Transaction> findById(Long id);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

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

    List<Transaction> findAllByUserIdAndPeriod(
            Long userId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

}
