package br.com.beca.financeira.users.application.port.in;

public interface UserCsvImportUseCase {

    UserCsvImportResult importUsers(String csvContent);
}
