package br.com.beca.financeira.transactions.application.port.out;

import java.math.BigDecimal;

public interface AccountLimitsPort {
    AccountLimitsPort.AccountLimits findByUserId(Long userId);

    record AccountLimits(
            BigDecimal balance,
            BigDecimal dailyLimit,
            BigDecimal perTransactionLimit
    ) {
    }
}
