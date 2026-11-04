package br.com.beca.financeira.users.repository;

import br.com.beca.financeira.users.adapters.out.persistence.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    Optional<UserEntity> findByEmail(String email);
}
