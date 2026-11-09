package br.com.beca.financeira.transactions.adapters.in.web;


import br.com.beca.financeira.transactions.adapters.in.security.MsUsersAuthenticationProvider;
import br.com.beca.financeira.transactions.application.port.in.AccountLimitsUseCase;
import br.com.beca.financeira.transactions.application.port.in.ExpenseAnalysisUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionCsvExportUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.application.port.out.UserIdentityPort;
import br.com.beca.financeira.transactions.application.port.service.AccountLimitsUnavailableException;
import br.com.beca.financeira.transactions.application.port.service.AccountNotFoundException;
import br.com.beca.financeira.transactions.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.HttpClientErrorException;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.springframework.http.MediaType;

import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;

import java.time.LocalDate;
import java.math.BigDecimal;

import br.com.beca.financeira.transactions.application.port.service.TransactionNotFoundException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@Import({SecurityConfig.class, MsUsersAuthenticationProvider.class})
class TransactionSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionUseCase transactionUseCase;

    @MockitoBean
    private ExpenseAnalysisUseCase expenseAnalysisUseCase;

    @MockitoBean
    private TransactionCsvExportUseCase transactionCsvExportUseCase;

    @MockitoBean
    private UserIdentityPort userIdentityPort;

    @MockitoBean
    private AccountLimitsUseCase accountLimitsUseCase;

    @Test
    void shouldRejectRequestWithoutCredentials() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectExpenseAnalysisWithoutCredentials() throws Exception {
        mockMvc.perform(get("/transactions/analysis")
                        .param("userId", "7"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowRequestWithValidCredentials() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));
        when(transactionUseCase.findAllByUserIdAndFilters(7L, null, null, null))
                .thenReturn(List.of());

        mockMvc.perform(get("/transactions")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isOk());
        verify(transactionUseCase).findAllByUserIdAndFilters(7L, null, null, null);
    }

    private String basicAuth(String email, String password) {
        String credentials = email + ":" + password;
        String encodedCredentials = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encodedCredentials;
    }

    @Test
    void shouldRejectRequestWithInvalidCredentials() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaErrada!"))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED,
                        "Unauthorized",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        StandardCharsets.UTF_8
                ));

        mockMvc.perform(get("/transactions")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaErrada!")
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCreateTransactionForAuthenticatedUser() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));

        Transaction transaction = new Transaction(
                7L,
                "Compra teste",
                new BigDecimal("25.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );

        when(transactionUseCase.create(
                7L,
                "Compra teste",
                new BigDecimal("25.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        )).thenReturn(transaction);

        mockMvc.perform(post("/transactions")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Compra teste",
                                    "amount": 25.00,
                                    "type": "PURCHASE",
                                    "category": "Alimentação"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(7));

        verify(transactionUseCase).create(
                7L,
                "Compra teste",
                new BigDecimal("25.00"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );
    }

    @Test
    void shouldAnalyzeExpensesForAuthenticatedUser() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));

        when(expenseAnalysisUseCase.analyzeExpenses(7L, null, null))
                .thenReturn(new ExpenseAnalysis(
                        LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 30),
                        BigDecimal.ZERO,
                        List.of(),
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(get("/transactions/analysis")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isOk());

        verify(expenseAnalysisUseCase).analyzeExpenses(7L, null, null);
    }

    @Test
    void shouldNotExposeTransactionOwnedByAnotherUser() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));
        when(transactionUseCase.findByIdAndUserId(42L, 7L))
                .thenThrow(new TransactionNotFoundException("Transação não encontrada"));

        mockMvc.perform(get("/transactions/42")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isNotFound());

        verify(transactionUseCase).findByIdAndUserId(42L, 7L);
    }

    @Test
    void shouldReturnServiceUnavailableWhenMsUsersIsUnavailable() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenThrow(new AuthenticationServiceException("MS-Users está indisponível"));

        mockMvc.perform(get("/transactions")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldReturnBadRequestWhenPurchaseHasNoCategory() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));

        mockMvc.perform(post("/transactions")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Compra teste",
                                    "amount": 25.00,
                                    "type": "PURCHASE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros[0].campo").value("category"))
                .andExpect(jsonPath("$.erros[0].message")
                        .value("Categoria é obrigatória para compras"));

        verify(transactionUseCase, never()).create(
                anyLong(),
                anyString(),
                any(),
                any(),
                any(),
                any()
        );
    }

    @Test
    void shouldExportAuthenticatedUsersTransactionsAsCsv() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));

        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);

        Transaction transaction = new Transaction(
                1L,
                7L,
                "Compra; teste \"especial\"",
                new BigDecimal("25.00"),
                TransactionType.PURCHASE,
                LocalDate.of(2026, 11, 10).atTime(10, 15),
                "Alimentação",
                null,
                br.com.beca.financeira.transactions.domain.TransactionStatus.REJECTED,
                "Limite excedido",
                null,
                null,
                null,
                null,
                false
        );

        String csv = "\uFEFFdata;tipo;categoria;descricao;valor (R$);status;motivo_rejeicao\r\n"
                + "10-11-2026;\"PURCHASE\";\"Alimentação\";\"Compra; teste "
                + "\"\"especial\"\"\";25,00;\"REJECTED\";\"Limite excedido\"\r\n";

        when(transactionCsvExportUseCase.exportCsv(7L, start, end))
                .thenReturn(csv.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/transactions/export")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"transacoes.csv\""
                ))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data;tipo;categoria")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10-11-2026")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Alimentação")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("25,00")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"Compra; teste \"\"especial\"\"\"")));

        verify(transactionCsvExportUseCase).exportCsv(7L, start, end);
    }

    @Test
    void shouldReturnAccountLimitsForAuthenticatedUser() throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));
        when(accountLimitsUseCase.findForUser(7L))
                .thenReturn(new AccountLimitsUseCase.AccountLimitsView(
                        new BigDecimal("250.00"),
                        new BigDecimal("100.00"),
                        new BigDecimal("30.00")
                ));

        mockMvc.perform(get("/transactions/account-limits")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(250.0))
                .andExpect(jsonPath("$.dailyLimit").value(100.0))
                .andExpect(jsonPath("$.perTransactionLimit").value(30.0));

        verify(accountLimitsUseCase).findForUser(7L);
    }

    @Test
    void shouldReturnNotFoundWhenAuthenticatedUsersAccountDoesNotExist()
            throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));
        when(accountLimitsUseCase.findForUser(7L))
                .thenThrow(new AccountNotFoundException("Conta não encontrada"));

        mockMvc.perform(get("/transactions/account-limits")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isNotFound());

        verify(accountLimitsUseCase).findForUser(7L);
    }

    @Test
    void shouldReturnServiceUnavailableWhenAccountLimitsServiceIsUnavailable()
            throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));
        when(accountLimitsUseCase.findForUser(7L))
                .thenThrow(new AccountLimitsUnavailableException("Serviço indisponível"));

        mockMvc.perform(get("/transactions/account-limits")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isServiceUnavailable());

        verify(accountLimitsUseCase).findForUser(7L);
    }

    @Test
    void shouldFilterAuthenticatedUsersTransactionsByPeriodAndType()
            throws Exception {
        when(userIdentityPort.authenticate("ana@example.com", "SenhaTeste123!"))
                .thenReturn(new UserIdentityPort.AuthenticatedUser(7L, "ana@example.com"));

        LocalDate startDate = LocalDate.of(2026, 11, 1);
        LocalDate endDate = LocalDate.of(2026, 11, 30);

        when(transactionUseCase.findAllByUserIdAndFilters(
                7L, startDate, endDate, TransactionType.PURCHASE
        )).thenReturn(List.of());

        mockMvc.perform(get("/transactions")
                        .param("startDate", "2026-11-01")
                        .param("endDate", "2026-11-30")
                        .param("type", "PURCHASE")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(transactionUseCase).findAllByUserIdAndFilters(
                7L, startDate, endDate, TransactionType.PURCHASE
        );
    }
}
