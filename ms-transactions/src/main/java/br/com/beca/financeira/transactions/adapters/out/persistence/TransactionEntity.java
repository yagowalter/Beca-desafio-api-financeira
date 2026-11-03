package br.com.beca.financeira.transactions.adapters.out.persistence;

import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionStatus;
import br.com.beca.financeira.transactions.domain.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "category")
    private String category;

    @Column(name = "destination_user_id")
    private Long destinationUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private TransactionStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "usd_rate", precision = 19, scale = 6)
    private BigDecimal usdRate;

    @Column(name = "usd_quote_date")
    private LocalDate usdQuoteDate;

    @Column(name = "eur_rate", precision = 19, scale = 6)
    private BigDecimal eurRate;

    @Column(name = "eur_quote_date")
    private LocalDate eurQuoteDate;

    @Column(name = "exchange_rates_available", nullable = false)
    private boolean exchangeRatesAvailable;

    protected TransactionEntity() {
    }

    public TransactionEntity(Transaction transaction) {
        this.id = transaction.getId();
        this.userId = transaction.getUserId();
        this.description = transaction.getDescription();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.occurredAt = transaction.getOccurredAt();
        this.category = transaction.getCategory();
        this.destinationUserId = transaction.getDestinationUserId();
        this.status = transaction.getStatus();
        this.rejectionReason = transaction.getRejectionReason();
        this.usdRate = transaction.getUsdRate();
        this.usdQuoteDate = transaction.getUsdQuoteDate();
        this.eurRate = transaction.getEurRate();
        this.eurQuoteDate = transaction.getEurQuoteDate();
        this.exchangeRatesAvailable = transaction.isExchangeRatesAvailable();
    }

    public Transaction toDomain() {
        return new Transaction(
                id,
                userId,
                description,
                amount, type,
                occurredAt,
                category,
                destinationUserId,
                status,
                rejectionReason,
                usdRate,
                usdQuoteDate,
                eurRate,
                eurQuoteDate,
                exchangeRatesAvailable
        );
    }

}