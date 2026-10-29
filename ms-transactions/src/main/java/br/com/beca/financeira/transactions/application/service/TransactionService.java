package br.com.beca.financeira.transactions.application.service;

import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionService implements TransactionUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;

    public TransactionService(TransactionRepositoryPort transactionRepositoryPort) {
        this.transactionRepositoryPort = transactionRepositoryPort;
    }

    @Override
    public Transaction create(Long userId, String description, BigDecimal amount, TransactionType type) {
        Transaction transaction = new Transaction(
                userId, description, amount, type
        );

        return transactionRepositoryPort.save(transaction);
    }

    @Override
    public List<Transaction> findAll() {
        return transactionRepositoryPort.findAll();
    }

    @Override
    public Transaction findById(Long id) {
        return findEntityById(id);
    }

    private Transaction findEntityById(Long id) {
        return transactionRepositoryPort.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transação não encontrada")
                );
    }

    @Override
    public Transaction update(Long id, String description, BigDecimal amount, TransactionType type) {

        Transaction transaction = findEntityById(id);

        transaction.update(description, amount, type);

        return transactionRepositoryPort.save(transaction);
    }

    @Override
    public void delete(Long id) {
        findEntityById(id);
        transactionRepositoryPort.deleteById(id);
    }
}
