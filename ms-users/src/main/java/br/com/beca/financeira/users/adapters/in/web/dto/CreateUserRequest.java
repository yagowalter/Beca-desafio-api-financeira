package br.com.beca.financeira.users.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nome completo do usuário", example = "Ana Silva")
    private String name;

    @NotBlank
    @Email
    @Size(max = 150)
    @Schema(description = "E-mail usado para login", example = "ana.silva@example.com")
    private String email;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, message = "Senha deve ter no mínimo 8 caracteres")
    @Schema(description = "Senha com no mínimo 8 caracteres", example = "SenhaSegura123!")
    private String password;

    public CreateUserRequest() {
    }

    public CreateUserRequest(String name, String email) {
        this.name = name;
        setEmail(email);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}


