package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.application.port.out.ExchangeRatePort;
import br.com.beca.financeira.transactions.application.port.out.TransactionRepositoryPort;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.springframework.stereotype.Service;
import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import org.springframework.web.client.RestClientException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TransactionService implements TransactionUseCase {

    private final TransactionRepositoryPort transactionRepositoryPort;
    private final AccountLimitsPort accountLimitsPort;
    private final ExchangeRatePort exchangeRatePort;

    public TransactionService(
            TransactionRepositoryPort transactionRepositoryPort,
            AccountLimitsPort accountLimitsPort,
            ExchangeRatePort exchangeRatePort
    ) {
        this.transactionRepositoryPort = transactionRepositoryPort;
        this.accountLimitsPort = accountLimitsPort;
        this.exchangeRatePort = exchangeRatePort;
    }

    @Override
    public Transaction create(
            Long userId,
            String description,
            BigDecimal amount,
            TransactionType type,
            String category,
            Long destinationUserId
    ) {
        Transaction transaction = new Transaction(
                userId, description, amount, type, category, destinationUserId
        );

        registerLatestAvailableExchangeRates(transaction);

        if (type == TransactionType.WITHDRAW
                || type == TransactionType.TRANSFER
                || type == TransactionType.PURCHASE) {
            AccountLimitsPort.AccountLimits account =
                    accountLimitsPort.findByUserId(userId);

            String rejectionReason =
                    transaction.validateBalanceAndTransactionLimit(
                            account.balance(),
                            account.perTransactionLimit()
                    );

            if (rejectionReason == null) {
                LocalDate today = LocalDate.now();

                BigDecimal spentToday =
                        transactionRepositoryPort.sumApprovedOutflowsByUserIdAndPeriod(
                                userId,
                                today.atStartOfDay(),
                                today.plusDays(1).atStartOfDay()
                        );

                rejectionReason = transaction.validateDailyLimit(
                        spentToday,
                        account.dailyLimit()
                );
            }

            if (rejectionReason != null) {
                transaction.reject(rejectionReason);
            } else {
                transaction.approve();
            }
        }

        if (type == TransactionType.DEPOSIT) {
            transaction.approve();
        }


        return transactionRepositoryPort.save(transaction);
    }

    private void registerLatestAvailableExchangeRates(Transaction transaction) {
        LocalDate quoteDate = transaction.getOccurredAt().toLocalDate();

        for (int daysBack = 0; daysBack <= 7; daysBack++) {
            LocalDate candidateDate = quoteDate.minus(daysBack, ChronoUnit.DAYS);

            try {
                ExchangeRatePort.ExchangeRate usd =
                        exchangeRatePort.findByCurrencyAndDate("USD", candidateDate);
                ExchangeRatePort.ExchangeRate eur =
                        exchangeRatePort.findByCurrencyAndDate("EUR", candidateDate);

                transaction.registerExchangeRates(
                        usd.sellingRate(),
                        usd.quoteDate(),
                        eur.sellingRate(),
                        eur.quoteDate()
                );
                return;
            } catch (RestClientException | IllegalStateException exception) {
                // Sem cotação nessa data: tenta a data anterior.
            }
        }

        transaction.registerExchangeRates(null, null, null, null);
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

}