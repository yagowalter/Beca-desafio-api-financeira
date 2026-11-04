package br.com.beca.financeira.users.application.service;


import br.com.beca.financeira.users.application.port.in.UserCsvImportResult;
import br.com.beca.financeira.users.application.port.in.UserUseCase;
import jakarta.validation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserCsvImportServiceTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private UserUseCase userUseCase;

    private UserCsvImportService service;

    @BeforeAll
    static void configureValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void configureService() {
        service = new UserCsvImportService(userUseCase, validator);
    }

    @Test
    void shouldRejectInvalidHeader() {
        assertThrows(
                InvalidCsvHeaderException.class,
                () -> service.importUsers("nome;email\nAna;ana@email.com")
        );

        verifyNoInteractions(userUseCase);
    }

    @Test
    void shouldIgnoreBlankLineAndImportNameWithSemicolonInsideQuotes() {
        UserCsvImportResult result = service.importUsers(
                "name;email;password\n\n\"Silva; Júnior\";ana@email.com;SenhaTeste123!"
        );

        assertEquals(1, result.importedCount());
        assertEquals(0, result.failedCount());
        verify(userUseCase).create(
                "Silva; Júnior",
                "ana@email.com",
                "SenhaTeste123!"
        );
    }

    @Test
    void shouldRecordDatabaseConstraintViolationAndContinueImport() {
        doThrow(new DataIntegrityViolationException("restrição do banco"))
                .when(userUseCase).create("Ana", "ana@email.com", "SenhaAna123!");

        UserCsvImportResult result = service.importUsers(
                "name;email;password\n"
                        + "Ana;ana@email.com;SenhaAna123!\n"
                        + "Bruno;bruno@email.com;SenhaBruno123!"
        );

        assertEquals(1, result.importedCount());
        assertEquals(1, result.failedCount());
        assertEquals(2, result.errors().getFirst().lineNumber());
        assertEquals(
                "Dados do usuário violam uma restrição do sistema",
                result.errors().getFirst().message()
        );

        verify(userUseCase).create("Bruno", "bruno@email.com", "SenhaBruno123!");
    }

    @Test
    void shouldRecordDuplicateEmailAndContinueImport() {
        doThrow(new DuplicateResourceException("E-mail já cadastrado"))
                .when(userUseCase).create("Ana", "ana@email.com", "SenhaAna123!");

        UserCsvImportResult result = service.importUsers(
                "name;email;password\n"
                        + "Ana;ana@email.com;SenhaAna123!\n"
                        + "Bruno;bruno@email.com;SenhaBruno123!"
        );

        assertEquals(1, result.importedCount());
        assertEquals(1, result.failedCount());
        assertEquals(2, result.errors().getFirst().lineNumber());
        assertEquals(
                "E-mail já cadastrado",
                result.errors().getFirst().message()
        );

        verify(userUseCase).create("Bruno", "bruno@email.com", "SenhaBruno123!");
    }

    @Test
    void shouldRejectCsvRowWithoutPassword(){
        UserCsvImportResult result = service.importUsers(
                "name;email;password\nAna;ana@email.com;"
        );

        assertEquals(0, result.importedCount());
        assertEquals(1, result.failedCount());
        assertTrue(result.errors().getFirst().message().contains("Senha é obrigatória"));
        verifyNoInteractions(userUseCase);
    }

    @Test
    void shouldRejectRowWithShortPassword() {
        UserCsvImportResult result = service.importUsers(
                "name;email;password\nAna;ana@example.com;curta"
        );

        assertEquals(0, result.importedCount());
        assertEquals(1, result.failedCount());
        assertEquals(2, result.errors().getFirst().lineNumber());
        verifyNoInteractions(userUseCase);
    }
}
