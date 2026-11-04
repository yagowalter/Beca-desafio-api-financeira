package br.com.beca.financeira.transactions.adapters.in.web;


import br.com.beca.financeira.transactions.adapters.in.security.MsUsersAuthenticationProvider;
import br.com.beca.financeira.transactions.application.port.in.ExpenseAnalysisUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.application.port.out.UserIdentityPort;
import br.com.beca.financeira.transactions.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    private UserIdentityPort userIdentityPort;

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
        when(transactionUseCase.findAllByUserId(7L)).thenReturn(List.of());

        mockMvc.perform(get("/transactions")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                basicAuth("ana@example.com", "SenhaTeste123!")
                        ))
                .andExpect(status().isOk());
        verify(transactionUseCase).findAllByUserId(7L);
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
}
