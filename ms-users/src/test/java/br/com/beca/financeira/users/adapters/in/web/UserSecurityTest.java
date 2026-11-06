package br.com.beca.financeira.users.adapters.in.web;


import br.com.beca.financeira.users.adapters.in.security.EmailUserDetailsService;
import br.com.beca.financeira.users.application.port.in.UserCsvImportUseCase;
import br.com.beca.financeira.users.application.port.in.UserUseCase;
import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import br.com.beca.financeira.users.config.PasswordConfig;
import br.com.beca.financeira.users.config.SecurityConfig;
import br.com.beca.financeira.users.domain.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, PasswordConfig.class, EmailUserDetailsService.class})
class UserSecurityTest {

    private static final String EMAIL = "ana@example.com";
    private static final String PASSWORD = "SenhaDeTeste123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UserRepositoryPort repository;

    @MockitoBean
    private UserUseCase userUseCase;

    @MockitoBean
    private UserCsvImportUseCase userCsvImportUseCase;

    @Test
    void shouldCreateUserWithoutExposingPassword() throws Exception {
        when(userUseCase.create("Ana", EMAIL, PASSWORD))
                .thenReturn(new User(1L, "Ana", EMAIL, passwordEncoder.encode(PASSWORD)));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Ana",
                                    "email": "ana@example.com",
                                    "password": "SenhaDeTeste123!"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());
    }

    @Test
    void shouldRejectUserCreationWithShortPassword() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Ana",
                                "email": "ana@example.com",
                                "password": "curta"
                            }
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectRequestWithoutCredentials() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldForbidViewingAnotherUsersAccount() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);

        mockMvc.perform(get("/users/2")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isForbidden());

        verify(userUseCase, never()).findById(2L);
    }


    @Test
    void shouldReturnAuthenticatedUserProfile() throws Exception {
        User user = userWithPasswordHash();

        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userUseCase.findByEmail(EMAIL)).thenReturn(user);

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.password").doesNotHaveJsonPath())
                .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());
    }

    @Test
    void shouldRejectRequestWithWrongPassword() throws Exception {
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithPasswordHash()));

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth("SenhaErrada123!")))
                .andExpect(status().isUnauthorized());
    }

    private User userWithPasswordHash() {
        return new User(1L, "Ana", EMAIL, passwordEncoder.encode(PASSWORD));
    }

    private String basicAuth(String password) {
        String credentials = EMAIL + ":" + password;
        return "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldForbidUpdatingAnotherUsersAccount() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);

        mockMvc.perform(put("/users/2")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Bruno Atualizado",
                                "email": "bruno.atualizado@example.com"
                            }
                            """))
                .andExpect(status().isForbidden());

        verify(userUseCase, never()).findById(2L);
        verify(userUseCase, never()).update(anyLong(), anyString(), anyString());
    }

    @Test
    void shouldAllowUpdatingOwnAccount() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);
        when(userUseCase.update(1L, "Ana Atualizada", "ana.atualizada@example.com"))
                .thenReturn(new User(
                        1L,
                        "Ana Atualizada",
                        "ana.atualizada@example.com",
                        passwordEncoder.encode(PASSWORD)
                ));

        mockMvc.perform(put("/users/1")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Ana Atualizada",
                                "email": "ana.atualizada@example.com"
                            }
                            """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana.atualizada@example.com"));

        verify(userUseCase).update(1L, "Ana Atualizada", "ana.atualizada@example.com");
    }

    @Test
    void shouldForbidDeletingAnotherUsersAccount() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);

        mockMvc.perform(delete("/users/2")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isForbidden());

        verify(userUseCase, never()).findById(2L);
        verify(userUseCase, never()).delete(anyLong());
    }

    @Test
    void shouldAllowDeletingOwnAccount() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);

        mockMvc.perform(delete("/users/1")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isNoContent());

        verify(userUseCase).delete(1L);
    }


    @Test
    void shouldAllowViewingOwnAccount() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);
        when(userUseCase.findById(1L))
                .thenReturn(authenticatedUser);

        mockMvc.perform(get("/users/1")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.passwordHash").doesNotHaveJsonPath());
    }

    @Test
    void shouldForbidViewingNonexistentUserId() throws Exception {
        User authenticatedUser = userWithPasswordHash();

        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(authenticatedUser));
        when(userUseCase.findByEmail(EMAIL))
                .thenReturn(authenticatedUser);

        mockMvc.perform(get("/users/999999")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isForbidden());

        verify(userUseCase, never()).findById(999999L);
    }

}
