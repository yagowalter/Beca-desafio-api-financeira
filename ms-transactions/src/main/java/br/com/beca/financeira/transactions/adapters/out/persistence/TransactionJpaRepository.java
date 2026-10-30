package br.com.beca.financeira.transactions.adapters.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionJpaRepository extends JpaRepository<br.com.beca.financeira.transactions.adapters.out.persistence.TransactionEntity, Long> {
}