package br.com.beca.financeira.users.application.service;

import br.com.beca.financeira.users.application.port.in.UserCsvImportResult;
import br.com.beca.financeira.users.application.port.in.UserCsvImportUseCase;
import br.com.beca.financeira.users.application.port.in.UserUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserCsvImportService implements UserCsvImportUseCase {

    private final UserUseCase userUseCase;
    private final Validator validator;

    private static final CSVFormat CSV_FORMAT = CSVFormat.DEFAULT.builder()
            .setDelimiter(";")
            .get();

    public UserCsvImportService(UserUseCase userUseCase, Validator validator) {
        this.userUseCase = userUseCase;
        this.validator = validator;
    }

    @Override
    public UserCsvImportResult importUsers(String csvContent) {
        List<String> lines = csvContent == null
                ? List.of()
                : csvContent.lines().toList();

        if (lines.isEmpty()
                || !lines.getFirst().replace("\uFEFF", "").trim()
                .equalsIgnoreCase("name;email")) {
            throw new InvalidCsvHeaderException("Cabeçalho inválido. Use name;email");
        }

        List<UserCsvImportResult.RowError> errors = new ArrayList<>();
        int importedCount = 0;

        for (int index = 1; index < lines.size(); index++) {
            int lineNumber = index + 1;
            String line = lines.get(index);

            if (line.isBlank()) {
                continue;
            }

            if (processLine(line, lineNumber, errors)) {
                importedCount++;
            }
        }

        return new UserCsvImportResult(
                importedCount,
                errors.size(),
                errors
        );
    }

    private boolean processLine(
            String line,
            int lineNumber,
            List<UserCsvImportResult.RowError> errors
    ) {

        CSVRecord record;

        try (CSVParser parser = CSVParser.parse(line, CSV_FORMAT)) {
            List<CSVRecord> records = parser.getRecords();

            if (records.size() != 1 || records.getFirst().size() != 2) {
                errors.add(new UserCsvImportResult.RowError(
                        lineNumber,
                        "A linha deve conter nome e e-mail separados por ponto e vírgula"
                ));
                return false;
            }

            record = records.getFirst();
        } catch (IOException exception) {
            errors.add(new UserCsvImportResult.RowError(
                    lineNumber,
                    "Formato CSV inválido"
            ));
            return false;
        }

        CsvUserRow row = new CsvUserRow(
                record.get(0).trim(),
                record.get(1).trim()
        );

        Set<ConstraintViolation<CsvUserRow>> violations = validator.validate(row);

        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("; "));
            errors.add(new UserCsvImportResult.RowError(lineNumber, message));
            return false;
        }

        try {
            userUseCase.create(row.name(), row.email());
            return true;
        } catch (DuplicateResourceException exception) {
            errors.add(new UserCsvImportResult.RowError(
                    lineNumber,
                    exception.getMessage()
            ));
            return false;
        } catch (DataIntegrityViolationException exception) {
            errors.add(new UserCsvImportResult.RowError(
                    lineNumber,
                    "Dados do usuário violam uma restrição do sistema"));
            return false;
        }
    }

    private record CsvUserRow(
            @NotBlank(message = "Nome é obrigatório")
            @Size(max = 100, message = "Nome deve ter até 100 caracteres") String name,

            @NotBlank(message = "E-mail é obrigatório")
            @Email(message = "E-mail inválido")
            @Size(max = 150, message = "E-mail deve ter até 150 caracteres") String email
    ) {
    }
}
