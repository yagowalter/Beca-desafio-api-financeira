package br.com.beca.financeira.transactions.adapters.in.security;

import br.com.beca.financeira.transactions.application.port.out.UserIdentityPort;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class MsUsersAuthenticationProvider implements AuthenticationProvider {

    private final UserIdentityPort userIdentityPort;

    public MsUsersAuthenticationProvider(UserIdentityPort userIdentityPort) {
        this.userIdentityPort = userIdentityPort;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        try {
            UserIdentityPort.AuthenticatedUser user =
                    userIdentityPort.authenticate(email, password);

            UserPrincipal principal = new UserPrincipal(user.id(), user.email());

            return new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        } catch (HttpClientErrorException.Unauthorized
                 | HttpClientErrorException.Forbidden
                 | HttpClientErrorException.NotFound exception) {
            throw new BadCredentialsException("Credenciais inválidas");
        } catch (ResourceAccessException exception) {
            throw new AuthenticationServiceException(
                    "MS-Users está indisponível", exception
            );
        } catch (RestClientException exception) {
            throw new AuthenticationServiceException(
                    "Falha ao validar credenciais no MS-Users", exception
            );
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    public record UserPrincipal(Long id, String email) {
    }
}
