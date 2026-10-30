package br.com.beca.financeira.transactions.application.port.out;

import br.com.beca.financeira.transactions.domain.Transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionRepositoryPort {

    Transaction save(Transaction transaction);

    List<Transaction> findAll();

    Optional<Transaction> findById(Long id);

    void deleteById(Long id);
}