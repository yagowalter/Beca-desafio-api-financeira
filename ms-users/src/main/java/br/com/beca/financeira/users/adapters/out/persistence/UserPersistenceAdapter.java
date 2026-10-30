package br.com.beca.financeira.users.adapters.out.persistence;

import br.com.beca.financeira.users.adapters.out.persistence.UserEntity;
import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import br.com.beca.financeira.users.domain.User;
import br.com.beca.financeira.users.repository.UserJpaRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    private final UserJpaRepository userJpaRepository;

    public UserPersistenceAdapter(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public User save(User user){
        UserEntity entity = new UserEntity(user);
        return userJpaRepository.save(entity).toDomain();
    }

    @Override
    public List<User> findAll(){
        return userJpaRepository.findAll()
                .stream()
                .map(UserEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<User> findById(Long id){
        return userJpaRepository.findById(id)
                .map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email){
        return userJpaRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByEmailAndIdNot(String email, Long id){
        return  userJpaRepository.existsByEmailAndIdNot(email, id);
    }

    @Override
    public void deleteById(Long id){
        userJpaRepository.deleteById(id);
    }

}
