package br.com.beca.financeira.users.adapters.in.security;

import br.com.beca.financeira.users.application.port.out.UserRepositoryPort;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Locale;

@NullMarked
@Component
public class EmailUserDetailsService implements UserDetailsService {

    private final UserRepositoryPort userRepositoryPort;

    public EmailUserDetailsService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        var user = userRepositoryPort.findByEmail(normalizedEmail)
                .filter(foundUser -> foundUser.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles("USER")
                .build();
    }
}
