package br.com.beca.financeira.users.adapters.out.persistence;

import br.com.beca.financeira.users.domain.User;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class UserEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    protected UserEntity() {
    }

    public UserEntity(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
    }

    public User toDomain() {
        return new User(this.id, this.name, this.email, this.passwordHash);
    }
}
