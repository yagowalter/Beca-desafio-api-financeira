package br.com.beca.financeira.transactions.adapters.out.persistence;


import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository transactionJpaRepository;

    public TransactionPersistenceAdapter(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
    }

    @Override
    public List<Transaction> findAllByUserIdAndType(
            Long userId,
            TransactionType type
    ) {
        return transactionJpaRepository.findAllByUserIdAndType(userId, type)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByUserIdAndTypeAndPeriod(
            Long userId,
            TransactionType type,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) {
        return transactionJpaRepository
                .findAllByUserIdAndTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId,
                        type,
                        startInclusive,
                        endExclusive
                )
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = new TransactionEntity(transaction);
        TransactionEntity savedEntity = transactionJpaRepository.save(entity);

        return savedEntity.toDomain();
    }

    @Override
    public List<Transaction> findAll() {
        return transactionJpaRepository.findAll()
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByUserId(Long userId) {
        return transactionJpaRepository.findAllByUserId(userId)
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return transactionJpaRepository.findById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public Optional<Transaction> findByIdAndUserId(Long id, Long userId) {
        return transactionJpaRepository.findByIdAndUserId(id, userId)
                .map(TransactionEntity::toDomain);
    }

    @Override
    public BigDecimal sumApprovedOutflowsByUserIdAndPeriod(
            Long userId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return transactionJpaRepository.sumApprovedOutflowsByUserIdAndPeriod(
                userId,
                start,
                end
        );
    }

    @Override
    public List<Transaction> findApprovedOutflowsByUserIdAndPeriod(
            Long userId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) {
        return transactionJpaRepository
                .findApprovedOutflowsByUserIdAndPeriod(
                        userId,
                        startInclusive,
                        endExclusive
                )
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }

    @Override
    public List<Transaction> findAllByUserIdAndPeriod(
            Long userId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) {
        return transactionJpaRepository
                .findAllByUserIdAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                        userId,
                        startInclusive,
                        endExclusive
                )
                .stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }
}
