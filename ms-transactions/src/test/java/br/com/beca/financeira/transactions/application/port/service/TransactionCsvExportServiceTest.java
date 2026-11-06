
package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionDomainException;
import br.com.beca.financeira.transactions.domain.TransactionType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionCsvExportServiceTest {

    private final TransactionUseCase transactionUseCase =
            mock(TransactionUseCase.class);

    private final TransactionCsvExportService service =
            new TransactionCsvExportService(transactionUseCase);

    private final LocalDate startDate = LocalDate.of(2026, 11, 1);
    private final LocalDate endDate = LocalDate.of(2026, 11, 30);

    private Transaction createTransaction(String description) {
        Transaction transaction = new Transaction(
                7L,
                description,
                new BigDecimal("25.50"),
                TransactionType.PURCHASE,
                "Alimentação",
                null
        );
        transaction.approve();
        return transaction;
    }

    private String export(Transaction... transactions) {
        when(transactionUseCase.findAllByUserIdAndPeriod(
                7L, startDate, endDate
        )).thenReturn(List.of(transactions));

        return new String(
                service.exportCsv(7L, startDate, endDate),
                StandardCharsets.UTF_8
        );
    }

    @Test
    void shouldExportCsvWithHeaderAndUtf8Bom() {
        String csv = export(createTransaction("Compra no mercado"));

        assertTrue(csv.startsWith(
                "\uFEFFdata;tipo;categoria;descricao;valor (R$);status;motivo_rejeicao\r\n"
        ));

        assertTrue(csv.matches("(?s).*\"\\d{2}-\\d{2}-\\d{4}\";.*"));

        assertTrue(csv.contains(
                ";\"PURCHASE\";\"Alimentação\";\"Compra no mercado\";25,50;\"APPROVED\";"
        ));

        verify(transactionUseCase).findAllByUserIdAndPeriod(
                7L, startDate, endDate
        );
    }

    @Test
    void shouldProtectDescriptionsStartingWithFormulaCharacters() {
        for (String description : List.of(
                "=SUM(1+1)",
                "+SUM(1+1)",
                "-SUM(1+1)",
                "@SUM(1+1)"
        )) {
            String csv = export(createTransaction(description));

            assertTrue(
                    csv.contains("\"'" + description + "\""),
                    "Descrição não protegida: " + description
            );
        }
    }

    @Test
    void shouldEscapeDoubleQuotesInDescription() {
        String csv = export(createTransaction("Compra \"especial\""));

        assertTrue(csv.contains("\"Compra \"\"especial\"\"\""));
    }

    @Test
    void shouldPreserveSemicolonsInsideQuotedDescription() {
        String csv = export(createTransaction("Compra; mercado"));

        assertTrue(csv.contains("\"Compra; mercado\""));
    }

    @Test
    void shouldExportHeaderWhenThereAreNoTransactions() {
        String csv = export();

        assertEquals(
                "\uFEFFdata;tipo;categoria;descricao;valor (R$);status;motivo_rejeicao\r\n",
                csv
        );
    }

    @Test
    void shouldAllowExportPeriodOfExactly12Months() {
        LocalDate endDate = startDate.plusMonths(12);

        when(transactionUseCase.findAllByUserIdAndPeriod(
                7L, startDate, endDate
        )).thenReturn(List.of());

        byte[] csv = service.exportCsv(7L, startDate, endDate);

        assertNotNull(csv);
        verify(transactionUseCase).findAllByUserIdAndPeriod(
                7L, startDate, endDate
        );
    }

    @Test
    void shouldRejectExportPeriodLongerThan12Months() {
        LocalDate endDate = startDate.plusMonths(12).plusDays(1);

        assertThrows(
                TransactionDomainException.class,
                () -> service.exportCsv(7L, startDate, endDate)
        );

        verifyNoInteractions(transactionUseCase);
    }
}
