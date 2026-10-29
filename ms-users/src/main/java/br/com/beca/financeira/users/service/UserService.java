package br.com.beca.financeira.users.service;

import br.com.beca.financeira.users.api.CreateUserRequest;
import br.com.beca.financeira.users.api.UpdateUserRequest;
import br.com.beca.financeira.users.api.UserResponse;
import br.com.beca.financeira.users.domain.User;
import br.com.beca.financeira.users.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse create(CreateUserRequest createUserRequest) {
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }
        User user = new User(createUserRequest.getName(), createUserRequest.getEmail());

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public UserResponse update(Long id, UpdateUserRequest updateUserRequest) {
        User user = findEntityById(id);

        if (!user.getEmail().equals(updateUserRequest.getEmail())
                && userRepository.existsByEmail(updateUserRequest.getEmail())) {
            throw new DuplicateResourceException("E-mail já cadastrado!");
        }

        user.update(
                updateUserRequest.getName(),
                updateUserRequest.getEmail()
        );

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }

    public void delete(Long id) {
        User user = findEntityById(id);
        userRepository.delete(user);
    }
}
