package br.com.beca.financeira.users.application.port.out;

import br.com.beca.financeira.users.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    List<User> findAll();

    Optional<User> findById(Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    void deleteById(Long id);
}
