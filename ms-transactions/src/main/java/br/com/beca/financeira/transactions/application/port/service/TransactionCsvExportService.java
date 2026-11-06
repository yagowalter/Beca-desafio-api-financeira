package br.com.beca.financeira.transactions.application.port.service;

import br.com.beca.financeira.transactions.application.port.in.TransactionCsvExportUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.domain.Transaction;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class TransactionCsvExportService implements TransactionCsvExportUseCase {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TransactionUseCase transactionUseCase;

    public TransactionCsvExportService(TransactionUseCase transactionUseCase) {
        this.transactionUseCase = transactionUseCase;
    }

    @Override
    public byte[] exportCsv(Long userId, LocalDate startDate, LocalDate endDate) {
        List<Transaction> transactions =
                transactionUseCase.findAllByUserIdAndPeriod(
                        userId, startDate, endDate
                );

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("data;tipo;categoria;descricao;valor (R$);status;motivo_rejeicao\r\n");

        NumberFormat amountFormat =
                NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        amountFormat.setMinimumFractionDigits(2);
        amountFormat.setMaximumFractionDigits(2);

        for (Transaction transaction : transactions) {
            csv.append(csvField(transaction.getOccurredAt()
                            .toLocalDate().format(DATE_FORMAT))).append(';')
                    .append(csvField(transaction.getType().name())).append(";")
                    .append(csvField(transaction.getCategory())).append(";")
                    .append(csvField(transaction.getDescription())).append(";")
                    .append(amountFormat.format(transaction.getAmount())).append(";")
                    .append(csvField(transaction.getStatus() == null
                            ? "" : transaction.getStatus().name())).append(';')
                    .append(csvField(transaction.getRejectionReason()))
                    .append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }

        String safeValue = value;

        // Protege campos que podem ser interpretados como fórmulas.
        if (!safeValue.isEmpty()
                && "=+-@".indexOf(safeValue.charAt(0)) >= 0) {
            safeValue = "'" + safeValue;
        }

        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
