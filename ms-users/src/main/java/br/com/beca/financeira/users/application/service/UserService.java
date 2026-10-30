package br.com.beca.financeira.users.application.service;

import br.com.beca.financeira.users.application.port.in.UserUseCase;
import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import br.com.beca.financeira.users.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public UserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public User create(String name, String email) {
        if (userRepositoryPort.existsByEmail(email)) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }

        return userRepositoryPort.save(new User(name, email));
    }

    @Override
    public List<User> findAll() {
        return userRepositoryPort.findAll();
    }

    @Override
    public User findById(Long id) {
        return findEntityById(id);
    }

    @Override
    public User update(Long id, String name, String email) {
        User user = findEntityById(id);

        if (!user.getEmail().equals(email)
                && userRepositoryPort.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("E-mail já cadastrado");
        }

        user.update(name, email);
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
}