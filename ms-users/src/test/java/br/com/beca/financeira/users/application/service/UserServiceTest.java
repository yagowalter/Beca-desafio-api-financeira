package br.com.beca.financeira.users.application.service;

import br.com.beca.financeira.users.application.port.out.PasswordHasherPort;
import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import br.com.beca.financeira.users.domain.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Test
    void shouldSavePasswordHashWhenCreatingUser(){
        UserRepositoryPort repository = mock(UserRepositoryPort.class);
        PasswordHasherPort hasher = mock(PasswordHasherPort.class);
        UserService service = new UserService(repository, hasher);

        when(hasher.hash("SenhaDeTeste123!")).thenReturn("hash-de-teste");

        service.create("Ana" ,"ana@example.com" , "SenhaDeTeste123!");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(repository).save(savedUser.capture());
        assertEquals("hash-de-teste", savedUser.getValue().getPasswordHash());
    }
}
