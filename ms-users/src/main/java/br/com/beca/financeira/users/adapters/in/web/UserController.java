package br.com.beca.financeira.users.adapters.in.web;

import br.com.beca.financeira.users.adapters.in.web.dto.CreateUserRequest;
import br.com.beca.financeira.users.adapters.in.web.dto.UpdateUserRequest;
import br.com.beca.financeira.users.adapters.in.web.dto.UserResponse;
import br.com.beca.financeira.users.application.port.in.UserUseCase;
import br.com.beca.financeira.users.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody CreateUserRequest createUserRequest) {

        User user = userUseCase.create(
                createUserRequest.getName(),
                createUserRequest.getEmail()
        );

        UserResponse userResponse = toResponse(user);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(userResponse.getId())
                .toUri();

        return ResponseEntity.created(location).body(userResponse);
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
            @Valid @RequestBody UpdateUserRequest updateUserRequest) {

        User user = userUseCase.update(
                id,
                updateUserRequest.getName(),
                updateUserRequest.getEmail()
        );

        return toResponse(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
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