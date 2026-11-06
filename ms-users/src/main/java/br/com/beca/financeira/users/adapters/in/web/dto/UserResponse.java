package br.com.beca.financeira.users.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public class UserResponse {

    @Schema(description = "Identificador do usuário", example = "46")
    private Long id;

    @Schema(description = "Nome completo", example = "Ana Silva")
    private String name;

    @Schema(description = "E-mail do usuário", example = "ana.silva@example.com")
    private String email;

    public UserResponse(Long id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
}
