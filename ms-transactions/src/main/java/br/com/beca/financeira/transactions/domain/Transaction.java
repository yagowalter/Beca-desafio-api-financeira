package br.com.beca.financeira.transactions.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;


public class Transaction {

    private Long id;
    private Long userId;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime occurredAt;

    public Transaction(Long userId, String description, BigDecimal amount, TransactionType type) {

        validate(userId, description, amount, type);

        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.occurredAt = LocalDateTime.now();
    }

    public Transaction(
            Long id,
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type,
            LocalDateTime occurredAt
    ) {
        validate(userId, description, amount, type);

        if ((id == null) || (id <= 0)) {
            throw new IllegalArgumentException("Identificador Inválido");
        }

        if (occurredAt == null) {
            throw new IllegalArgumentException("Data da transação é obrigatória");
        }

        this.id = id;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.occurredAt = occurredAt;
    }

    private void validate(
            Long userId, String description, BigDecimal amount, TransactionType type
    ) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Usuário inválido!");
        }

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Descrição é obrigatória");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Valor deve ser maior do que zero");
        }

        if (type == null) {
            throw new IllegalArgumentException("Tipo da transação é obrigatório");
        }
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getType() {
        return type;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void update(
            String description,
            BigDecimal amount,
            TransactionType type
    ) {
        validate(this.userId, description, amount, type);

        this.description = description;
        this.amount = amount;
        this.type = type;
    }


}