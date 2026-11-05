package br.com.beca.financeira.users.application.service;

import br.com.beca.financeira.users.application.port.out.PasswordHasherPort;
import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import br.com.beca.financeira.users.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Optional;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void shouldSavePasswordHashWhenCreatingUser() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        when(hasher.hash("SenhaDeTeste123!")).thenReturn("hash-de-teste");

        service.create("Ana", "ana@example.com", "SenhaDeTeste123!");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(repository).save(savedUser.capture());
        assertEquals("hash-de-teste", savedUser.getValue().getPasswordHash());
    }

    @Test
    void shouldRejectDuplicateEmailBeforeHashingPassword() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        when(repository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> service.create("Ana", " ANA@example.com ", "SenhaDeTeste123!")
        );

        verify(hasher, never()).hash(anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void shouldReturnAllUsersFromRepository() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        List<User> users = List.of(
                new User(1L, "Ana", "ana@example.com", "hash")
        );
        when(repository.findAll()).thenReturn(users);

        assertSame(users, service.findAll());
        verify(repository).findAll();
        verifyNoInteractions(hasher);
    }

    @Test
    void shouldReturnUserById() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);
        User expected = new User(1L, "Ana", "ana@example.com", "hash");

        when(repository.findById(1L)).thenReturn(Optional.of(expected));

        assertSame(expected, service.findById(1L));
        verify(repository).findById(1L);
    }

    @Test
    void shouldThrowWhenUserIdDoesNotExist() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.findById(99L)
        );

        verify(repository).findById(99L);
    }

    @Test
    void shouldUpdateUserAndNormalizeEmail() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);
        User user = new User(1L, "Ana", "ana@example.com", "hash");

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.existsByEmailAndIdNot("ana.nova@example.com", 1L))
                .thenReturn(false);
        when(repository.save(user)).thenReturn(user);

        User updated = service.update(1L, "Ana Nova", " ANA.NOVA@example.com ");

        assertEquals("ana.nova@example.com", updated.getEmail());
        assertEquals("Ana Nova", updated.getName());
        verify(repository).save(user);
    }

    @Test
    void shouldRejectUpdateWithEmailAlreadyUsedByAnotherUser() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);
        User user = new User(1L, "Ana", "ana@example.com", "hash");

        when(repository.findById(1L)).thenReturn(Optional.of(user));
        when(repository.existsByEmailAndIdNot("bruno@example.com", 1L))
                .thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> service.update(1L, "Ana", "bruno@example.com")
        );

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDeleteExistingUser() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);
        User user = new User(1L, "Ana", "ana@example.com", "hash");

        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void shouldNotDeleteUserThatDoesNotExist() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.delete(99L)
        );

        verify(repository, never()).deleteById(99L);
    }

    @Test
    void shouldFindUserByEmailIgnoringCaseAndWhitespace() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);
        User user = new User(1L, "Ana", "ana@example.com", "hash");

        when(repository.findByEmail("ana@example.com"))
                .thenReturn(Optional.of(user));

        User result = service.findByEmail(" ANA@EXAMPLE.COM ");

        assertSame(user, result);
        verify(repository).findByEmail("ana@example.com");
    }

    @Test
    void shouldThrowWhenEmailDoesNotExist() {
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        when(repository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.findByEmail("missing@example.com")
        );

        verify(repository).findByEmail("missing@example.com");
    }
}
