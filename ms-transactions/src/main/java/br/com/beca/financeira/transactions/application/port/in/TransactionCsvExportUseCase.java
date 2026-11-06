package br.com.beca.financeira.transactions.application.port.in;

import java.time.LocalDate;

public interface TransactionCsvExportUseCase {

    byte[] exportCsv(
            Long userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
