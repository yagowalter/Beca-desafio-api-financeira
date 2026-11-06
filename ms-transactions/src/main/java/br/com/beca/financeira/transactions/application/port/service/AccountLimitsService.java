package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import org.springframework.stereotype.Service;
import br.com.beca.financeira.transactions.application.port.in.AccountLimitsUseCase;

@Service
public class AccountLimitsService implements AccountLimitsUseCase {

    private final AccountLimitsPort accountLimitsPort;

    public AccountLimitsService(AccountLimitsPort accountLimitsPort) {
        this.accountLimitsPort = accountLimitsPort;
    }

    @Override
    public AccountLimitsView findForUser(Long userId) {
        AccountLimitsPort.AccountLimits account =
                accountLimitsPort.findByUserId(userId);

        return new AccountLimitsView(
                account.balance(),
                account.dailyLimit(),
                account.perTransactionLimit()
        );
    }
}
