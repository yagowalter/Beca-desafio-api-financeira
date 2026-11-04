package br.com.beca.financeira.transactions.adapters.out.http;

import br.com.beca.financeira.transactions.application.port.out.UserIdentityPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class MsUsersIdentityAdapter implements UserIdentityPort {

    private final RestClient restClient;

    public MsUsersIdentityAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${ms-users.base-url}") String msUsersBaseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(msUsersBaseUrl)
                .build();
    }

    @Override
    public AuthenticatedUser authenticate(String email, String rawPassword) {
        String credentials = email + ":" + rawPassword;
        String basicToken = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        MsUserResponse user = restClient.get()
                .uri("/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + basicToken)
                .retrieve()
                .body(MsUserResponse.class);

        if (user == null || user.id() == null || user.email() == null) {
            throw new IllegalStateException("MS-Users retornou uma identidade inválida");
        }

        return new AuthenticatedUser(user.id(), user.email());
    }

    private record MsUserResponse(Long id, String email) {

    }
}
