package br.com.beca.financeira.users.adapters.in.web;

import br.com.beca.financeira.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.financeira.users.adapters.in.web.dto.UpdateUserRequest;
import br.com.beca.financeira.users.adapters.in.web.dto.UserResponse;
import br.com.beca.financeira.users.application.port.in.UserCsvImportResult;
import br.com.beca.financeira.users.application.port.in.UserCsvImportUseCase;
import br.com.beca.financeira.users.application.port.in.UserUseCase;
import br.com.beca.financeira.users.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;

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
        User targetUser = userUseCase.findById(id);

        if (!targetUser.getEmail().equalsIgnoreCase(principal.getName())) {
            throw new AccessDeniedException("Acesso negado a este usuário");
        }
    }

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

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserCsvImportResult> importCsv(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String csvContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        UserCsvImportResult result = userCsvImportUseCase.importUsers(csvContent);
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userUseCase.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return toResponse(userUseCase.findById(id));
    }

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