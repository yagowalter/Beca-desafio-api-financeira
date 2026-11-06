package br.com.beca.financeira.users.application.service;

import br.com.beca.financeira.users.application.port.in.UserUseCase;
import br.com.beca.financeira.users.application.port.out.PasswordHasherPort;
import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import br.com.beca.financeira.users.domain.User;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserService implements UserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordHasherPort passwordHasherPort;

    public UserService(
            UserRepositoryPort userRepositoryPort,
            PasswordHasherPort passwordHasherPort
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.passwordHasherPort = passwordHasherPort;
    }

    @Override
    public User create(String name, String email, String rawPassword) {
        String normalizedEmail = normalizeEmail(email);

        if (userRepositoryPort.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }

        String passwordHash = passwordHasherPort.hash(rawPassword);

        return userRepositoryPort.save(
                new User(null, name, normalizedEmail, passwordHash)
        );
    }

    @Override
    public User findById(Long id) {
        return findEntityById(id);
    }

    @Override
    public User findByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        return userRepositoryPort.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Usuário não encontrado"));
    }

    @Override
    public User update(Long id, String name, String email) {
        User user = findEntityById(id);
        String normalizedEmail = normalizeEmail(email);

        if (!normalizeEmail(user.getEmail()).equals(normalizedEmail)
                && userRepositoryPort.existsByEmailAndIdNot(normalizedEmail, id)) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }

        user.update(name, normalizedEmail);
        return userRepositoryPort.save(user);
    }

    @Override
    public void delete(Long id) {
        findEntityById(id);
        userRepositoryPort.deleteById(id);
    }

    private User findEntityById(Long id) {
        return userRepositoryPort.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Usuário não encontrado"));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}