package br.com.beca.financeira.users.application.port.in;

import br.com.beca.financeira.users.domain.User;

import java.util.List;


public interface UserUseCase {

    User create(String name, String email);

    List<User> findAll();

    User findById(Long id);

    User update(Long id, String name, String email);

    void delete(Long id);

}
