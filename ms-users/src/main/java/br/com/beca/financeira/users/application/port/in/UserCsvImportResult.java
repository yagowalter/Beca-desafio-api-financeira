package br.com.beca.financeira.users.application.port.in;

import java.util.List;

public record UserCsvImportResult (
        int importedCount,
        int failedCount,
        List<RowError> errors
) {
    public record RowError(int lineNumber, String message) {}
}
