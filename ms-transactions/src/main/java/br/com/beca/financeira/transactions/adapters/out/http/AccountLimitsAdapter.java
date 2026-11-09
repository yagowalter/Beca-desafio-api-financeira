package br.com.beca.financeira.transactions.adapters.out.http;


import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import br.com.beca.financeira.transactions.application.port.service.AccountLimitsUnavailableException;
import br.com.beca.financeira.transactions.application.port.service.AccountNotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;

@Component
public class AccountLimitsAdapter implements AccountLimitsPort {
    private final RestClient restClient;
    private final String accountsUrl;

    @Override
    public AccountLimitsPort.AccountLimits findByUserId(Long userId) {
        List<AccountLimitsResponse> accounts;

        try {
            accounts = restClient.get()
                    .uri(accountsUrl + "?userId={userId}", userId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (HttpClientErrorException.NotFound exception) {
            throw new AccountNotFoundException(
                    "Conta não encontrada para o usuário " + userId
            );
        } catch (RestClientException exception) {
            throw new AccountLimitsUnavailableException(
                    "Não foi possível consultar saldo e limites da conta",
                    exception
            );
        }

        if (accounts == null || accounts.isEmpty()) {
            throw new AccountNotFoundException(
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

    @Override
    public void updateBalance(Long userId, BigDecimal newBalance) {
        AccountLimitsPort.AccountLimits current = findByUserId(userId);

        AccountLimitsResponse updateAccount = new AccountLimitsResponse();
        updateAccount.setUserId(userId);
        updateAccount.setBalance(newBalance);
        updateAccount.setDailyLimit(current.dailyLimit());
        updateAccount.setPerTransactionLimit(current.perTransactionLimit());

        try {
            restClient.put()
                    .uri(accountsUrl + "/{userId}", userId)
                    .body(updateAccount)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new AccountLimitsUnavailableException(
                    "Não foi possível atualizar o saldo da conta",
                    exception
            );
        }

    }

    public AccountLimitsAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${mockapi.accounts-url}") String accountsUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.accountsUrl = accountsUrl;
    }
}
