package br.com.beca.financeira.transactions.adapters.out.persistence;


import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.Transaction;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository transactionJpaRepository;

    public TransactionPersistenceAdapter(TransactionJpaRepository transactionJpaRepository) {
        this.transactionJpaRepository = transactionJpaRepository;
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
    public Optional<Transaction> findById(Long id) {
        return transactionJpaRepository.findById(id).map(TransactionEntity::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        transactionJpaRepository.deleteById(id);
    }

}