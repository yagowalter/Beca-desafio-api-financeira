package br.com.beca.financeira.transactions.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public class Transaction {

    private Long id;
    private Long userId;
    private String description;
    private BigDecimal amount;
    private TransactionType type;
    private LocalDateTime occurredAt;
    private String category;
    private Long destinationUserId;
    private TransactionStatus status;
    private String rejectionReason;
    private BigDecimal usdRate;
    private LocalDate usdQuoteDate;
    private BigDecimal eurRate;
    private LocalDate eurQuoteDate;
    private boolean exchangeRatesAvailable;

    public Transaction(
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type
    ) {
        this(userId, description, amount, type, null, null);
    }

    public Transaction(
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            Long destinationUserId
    ) {
        validate(userId, description, amount, type);
        validateTypeDetails(type, category, destinationUserId);

        if (type == TransactionType.TRANSFER
                && userId.equals(destinationUserId)) {
            throw new TransactionDomainException(
                    "Não é permitido transferir para o próprio usuário"
            );
        }

        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.occurredAt = LocalDateTime.now();
        this.category = category;
        this.destinationUserId = destinationUserId;
        this.exchangeRatesAvailable = false;
    }

    public Transaction(
            Long id,
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type,
            LocalDateTime occurredAt
    ) {
        this(
                id,
                userId,
                description,
                amount,
                type,
                occurredAt,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
    }

    public Transaction(
            Long id,
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type,
            LocalDateTime occurredAt,
            String category,
            Long destinationUserId,
            TransactionStatus status,
            String rejectionReason,
            BigDecimal usdRate,
            LocalDate usdQuoteDate,
            BigDecimal eurRate,
            LocalDate eurQuoteDate,
            boolean exchangeRatesAvailable

    ) {
        validate(userId, description, amount, type);
        validateTypeDetails(type, category, destinationUserId);
        validateStatus(status, rejectionReason);

        if (id == null || id <= 0) {
            throw new TransactionDomainException("Identificador invalido");
        }

        if (occurredAt == null) {
            throw new TransactionDomainException(
                    "Data da transação é obrigatória"
            );
        }

        this.id = id;
        this.userId = userId;
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.occurredAt = occurredAt;
        this.category = category;
        this.destinationUserId = destinationUserId;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.usdRate = usdRate;
        this.usdQuoteDate = usdQuoteDate;
        this.eurRate = eurRate;
        this.eurQuoteDate = eurQuoteDate;
        this.exchangeRatesAvailable = exchangeRatesAvailable;
    }

    public void registerExchangeRates(
            BigDecimal usdRate,
            LocalDate usdQuoteDate,
            BigDecimal eurRate,
            LocalDate eurQuoteDate
    ) {
        this.usdRate = usdRate;
        this.usdQuoteDate = usdQuoteDate;
        this.eurRate = eurRate;
        this.eurQuoteDate = eurQuoteDate;

        this.exchangeRatesAvailable = usdRate != null
                && eurRate != null
                && usdQuoteDate != null
                && eurQuoteDate != null;
    }

    private void validateTypeDetails(
            TransactionType type,
            String category,
            Long destinationUserId
    ) {
        if (type == TransactionType.PURCHASE
                && (category == null || category.isBlank())) {
            throw new TransactionDomainException(
                    "Categoria é obrigatória para compras"
            );
        }

        if (type == TransactionType.TRANSFER
                && (destinationUserId == null || destinationUserId <= 0)) {
            throw new TransactionDomainException(
                    "Usuário de destino é obrigatório para transferências"
            );
        }
    }

    private void validate(
            Long userId, String description, BigDecimal amount, TransactionType type
    ) {
        if (userId == null || userId <= 0) {
            throw new TransactionDomainException("Usuário inválido!");
        }

        if (description == null || description.isBlank()) {
            throw new TransactionDomainException("Descrição é obrigatória");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new TransactionDomainException("Valor deve ser maior do que zero");
        }

        if (type == null) {
            throw new TransactionDomainException("Tipo da transação é obrigatório");
        }
    }

    public String validateBalanceAndTransactionLimit(
            BigDecimal balance,
            BigDecimal perTransactionLimit
    ) {
        if (amount.compareTo(balance) > 0) {
            return "Saldo insuficiente";
        }

        if (amount.compareTo(perTransactionLimit) > 0) {
            return "Valor acima do limite por transação";
        }

        return null;
    }

    public String validateDailyLimit(
            BigDecimal spentToday,
            BigDecimal dailyLimit
    ) {
        BigDecimal totalWithNewTransaction = spentToday.add(amount);

        if (totalWithNewTransaction.compareTo(dailyLimit) > 0) {
            return "Limite diário excedido";
        }

        return null;
    }

    public void approve() {
        ensureNotFinalized();

        this.status = TransactionStatus.APPROVED;
        this.rejectionReason = null;
    }

    public void reject(String reason) {
        ensureNotFinalized();
        validateStatus(TransactionStatus.REJECTED, reason);

        this.rejectionReason = reason;
        this.status = TransactionStatus.REJECTED;
    }

    private void ensureNotFinalized() {
        if (status != null) {
            throw new IllegalStateException(
                    "A transação ja foi finalizada"
            );
        }
    }

    private void validateStatus(
            TransactionStatus status,
            String rejectionReason
    ) {
        if (status == TransactionStatus.REJECTED) {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new TransactionDomainException(
                        "Motivo da rejeição é obrigatório"
                );
            }
        } else if (rejectionReason != null) {
            throw new TransactionDomainException(
                    "Somente transações rejeitadas podem ter motivo de rejeição"
            );
        }
    }

    public BigDecimal getAmountUsd() {
        if (!exchangeRatesAvailable) {
            return null;
        }
        return amount.divide(usdRate, 2, java.math.RoundingMode.HALF_UP);
    }

    public BigDecimal getAmountEur() {
        if (!exchangeRatesAvailable) {
            return null;
        }
        return amount.divide(eurRate, 2, java.math.RoundingMode.HALF_UP);
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

    public String getCategory() {
        return category;
    }

    public Long getDestinationUserId() {
        return destinationUserId;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public LocalDate getUsdQuoteDate() {
        return usdQuoteDate;
    }

    public BigDecimal getEurRate() {
        return eurRate;
    }

    public BigDecimal getUsdRate() {
        return usdRate;
    }

    public LocalDate getEurQuoteDate() {
        return eurQuoteDate;
    }

    public boolean isExchangeRatesAvailable() {
        return exchangeRatesAvailable;
    }


}