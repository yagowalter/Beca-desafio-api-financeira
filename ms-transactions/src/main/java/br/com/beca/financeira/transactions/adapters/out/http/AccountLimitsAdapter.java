package br.com.beca.financeira.transactions.adapters.out.http;


import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class AccountLimitsAdapter implements AccountLimitsPort {
    private final RestClient restClient;
    private final String accountsUrl;

    @Override
    public AccountLimitsPort.AccountLimits findByUserId(Long userId) {
        List<AccountLimitsResponse> accounts = restClient.get()
                .uri(accountsUrl + "?userId={userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });

        if (accounts == null || accounts.isEmpty()) {
            throw new IllegalArgumentException(
                    "Conta não encontrada para o usuário " + userId
            );
        }

        AccountLimitsResponse account = accounts.get(0);

        return new AccountLimitsPort.AccountLimits(
                account.getBalance(),
                account.getDailyLimit(),
                account.getPerTransactionLimit()
        );
    }

    public AccountLimitsAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${mockapi.accounts-url}") String accountsUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.accountsUrl = accountsUrl;
    }
}
