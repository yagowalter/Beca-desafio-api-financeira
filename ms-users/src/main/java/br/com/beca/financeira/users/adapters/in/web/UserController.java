package br.com.beca.financeira.users.adapters.in.web;

import br.com.beca.financeira.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.financeira.users.adapters.in.web.dto.UpdateUserRequest;
import br.com.beca.financeira.users.adapters.in.web.dto.UserResponse;
import br.com.beca.financeira.users.application.port.in.UserCsvImportResult;
import br.com.beca.financeira.users.application.port.in.UserCsvImportUseCase;
import br.com.beca.financeira.users.application.port.in.UserUseCase;
import br.com.beca.financeira.users.domain.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;

@Tag(name = "Usuários", description = "Cadastro, consulta, atualização e remoção de usuários")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserUseCase userUseCase;
    private final UserCsvImportUseCase userCsvImportUseCase;

    public UserController(
            UserUseCase userUseCase,
            UserCsvImportUseCase userCsvImportUseCase
    ) {
        this.userUseCase = userUseCase;
        this.userCsvImportUseCase = userCsvImportUseCase;
    }

    private void ensureOwnAccount(Long id, Principal principal) {
        User authenticatedUser = userUseCase.findByEmail(principal.getName());

        if (!authenticatedUser.getId().equals(id)) {
            throw new AccessDeniedException("Acesso negado a este usuário");
        }
    }

    @Operation(
            summary = "Cadastrar usuário",
            description = "Cria um usuário com nome, e-mail e senha. O cadastro é público."
    )
    @ApiResponse(
            responseCode = "201",
            description = "Usuário cadastrado"
    )
    @ApiResponse(
            responseCode = "400",
            description = "Nome, e-mail ou senha inválidos",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "about:blank",
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Um ou mais campos estão inválidos",
                              "instance": "/users",
                              "erros": [
                                {
                                  "campo": "password",
                                  "mensagem": "Senha deve ter no mínimo 8 caracteres"
                                }
                              ]
                            }
                            """)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "E-mail já cadastrado",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class),
                    examples = @ExampleObject(value = """
                            {
                              "type": "about:blank",
                              "title": "Conflict",
                              "status": 409,
                              "detail": "E-mail já cadastrado",
                              "instance": "/users"
                            }
                            """)
            )
    )
    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest createUserRequest) {

        User user = userUseCase.create(
                createUserRequest.getName(),
                createUserRequest.getEmail(),
                createUserRequest.getPassword()
        );

        UserResponse userResponse = toResponse(user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(userResponse.getId())
                .toUri();

        return ResponseEntity.created(location).body(userResponse);
    }

    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Importar usuários por CSV",
            description = "Envie um arquivo CSV no campo 'file'. O cabeçalho obrigatório é name;email;password. Cada linha contém nome, e-mail e senha separados por ponto e vírgula."
    )
    @ApiResponse(responseCode = "200", description = "Resultado da importação, com quantidades importadas e erros por linha")
    @ApiResponse(responseCode = "400", description = "Cabeçalho CSV inválido")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserCsvImportResult> importCsv(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        UserCsvImportResult result = userCsvImportUseCase.importUsers(csvContent);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Consultar usuário autenticado",
            description = "Retorna o cadastro associado às credenciais Basic enviadas."
    )
    @ApiResponse(responseCode = "200", description = "Dados do usuário autenticado")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @SecurityRequirement(name = "basicAuth")
    @GetMapping("/me")
    public UserResponse findCurrentUser(Principal principal) {
        return toResponse(userUseCase.findByEmail(principal.getName()));
    }

    // GET /users/{id}
    @Operation(
            summary = "Consultar usuário por ID",
            description = "Retorna apenas o cadastro do próprio usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "403", description = "Acesso negado a outro usuário")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @SecurityRequirement(name = "basicAuth")
    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id, Principal principal) {
        ensureOwnAccount(id, principal);
        return toResponse(userUseCase.findById(id));
    }

    // PUT /users/{id}
    @Operation(summary = "Atualizar usuário",
            description = "Atualiza nome e e-mail do próprio cadastro. Requer autenticação Basic.")
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado tentou alterar outro cadastro")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @SecurityRequirement(name = "basicAuth")
    @PutMapping("/{id}")
    public UserResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest updateUserRequest, Principal principal) {

        ensureOwnAccount(id, principal);

        User user = userUseCase.update(
                id,
                updateUserRequest.getName(),
                updateUserRequest.getEmail()
        );

        return toResponse(user);
    }

    // DELETE /users/{id}
    @Operation(summary = "Excluir usuário",
            description = "Exclui o próprio cadastro. Requer autenticação Basic.")
    @ApiResponse(responseCode = "204", description = "Usuário excluído")
    @ApiResponse(responseCode = "403", description = "O usuário autenticado tentou excluir outro cadastro")
    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
    @SecurityRequirement(name = "basicAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        ensureOwnAccount(id, principal);

        userUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}