package br.com.beca.financeira.users.application.port.in;

import br.com.beca.financeira.users.domain.User;

public interface UserUseCase {

    User create(String name, String email, String rawPassword);

    User findById(Long id);

    User findByEmail(String email);

    User update(Long id, String name, String email);

    void delete(Long id);

}
