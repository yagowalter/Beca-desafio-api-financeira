package br.com.beca.financeira.transactions.application.port.in;

import java.math.BigDecimal;

public interface AccountLimitsUseCase {

    AccountLimitsView findForUser(Long userId);

    record AccountLimitsView(
            BigDecimal balance,
            BigDecimal dailyLimit,
            BigDecimal perTransactionLimit
    ) {

    }
}
