package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.in.AccountLimitsUseCase.AccountLimitsView;
import br.com.beca.financeira.transactions.application.port.out.AccountLimitsPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountLimitsServiceTest {

    private final AccountLimitsPort accountLimitsPort =
            mock(AccountLimitsPort.class);

    private final AccountLimitsService service =
            new AccountLimitsService(accountLimitsPort);

    @Test
    void shouldReturnLimitsForRequestedUser() {
        when(accountLimitsPort.findByUserId(52L)).thenReturn(
                new AccountLimitsPort.AccountLimits(
                        new BigDecimal("250.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("30.00")
                )
        );

        AccountLimitsView result = service.findForUser(52L);

        assertEquals(new BigDecimal("250.00"), result.balance());
        assertEquals(new BigDecimal("100.00"), result.dailyLimit());
        assertEquals(new BigDecimal("30.00"), result.perTransactionLimit());
        verify(accountLimitsPort).findByUserId(52L);
    }
}