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
    void shouldRejectRequestWithoutCredentials() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowRequestWithValidCredentials() throws Exception {
        User user = userWithPasswordHash();
        when(repository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(userUseCase.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMAIL))
                .andExpect(jsonPath("$[0].passwordHash").doesNotHaveJsonPath());
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
        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(userWithPasswordHash()));
        when(userUseCase.findById(2L))
                .thenReturn(new User(
                        2L,
                        "Bruno",
                        "bruno@example.com",
                        passwordEncoder.encode(PASSWORD)
                ));

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

        verify(userUseCase, never())
                .update(2L, "Bruno Atualizado", "bruno.atualizado2example.com");
    }

    @Test
    void shouldAllowUpdatingOwnAccount() throws Exception {
        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(userWithPasswordHash()));
        when(userUseCase.findById(1L))
                .thenReturn(userWithPasswordHash());
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
    }

    @Test
    void shouldForbidDeletingAnotherUsersAccount() throws Exception {
        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(userWithPasswordHash()));
        when(userUseCase.findById(2L))
                .thenReturn(new User(
                        2L,
                        "Bruno",
                        "bruno@example.com",
                        passwordEncoder.encode(PASSWORD)
                ));

        mockMvc.perform(delete("/users/2")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isForbidden());

        verify(userUseCase, never()).delete(2L);
    }

    @Test
    void shouldAllowDeletingOwnAccount() throws Exception {
        when(repository.findByEmail(EMAIL))
                .thenReturn(Optional.of(userWithPasswordHash()));
        when(userUseCase.findById(1L))
                .thenReturn(userWithPasswordHash());

        mockMvc.perform(delete("/users/1")
                        .header(HttpHeaders.AUTHORIZATION, basicAuth(PASSWORD)))
                .andExpect(status().isNoContent());

        verify(userUseCase).delete(1L);
    }

}
