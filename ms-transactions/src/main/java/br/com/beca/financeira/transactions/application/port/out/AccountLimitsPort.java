package br.com.beca.financeira.transactions.application.port.out;

import br.com.beca.financeira.transactions.adapters.out.http.AccountLimitsResponse;

import java.math.BigDecimal;

public interface AccountLimitsPort {

    AccountLimitsPort.AccountLimits findByUserId(Long userId);

    void updateBalance(Long userId, BigDecimal newBalance);

    record AccountLimits(
            BigDecimal balance,
            BigDecimal dailyLimit,
            BigDecimal perTransactionLimit
    ) {
    }
}
