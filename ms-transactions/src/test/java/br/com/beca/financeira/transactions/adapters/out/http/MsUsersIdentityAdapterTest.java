package br.com.beca.financeira.transactions.adapters.out.http;

import br.com.beca.financeira.transactions.application.port.out.UserIdentityPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MsUsersIdentityAdapterTest {

    @Test
    void shouldFetchAuthenticatedUserIdentityFromMsUsers() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(restClientBuilder).build();

        MsUsersIdentityAdapter adapter =
                new MsUsersIdentityAdapter(restClientBuilder, "http://localhost:8080");

        String credentials = "ana@example.com:SenhaTeste123!";
        String expectedBasicAuth = "Basic " + Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        server.expect(requestTo("http://localhost:8080/users/me"))
                .andExpect(method(GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, expectedBasicAuth))
                .andRespond(withSuccess(
                        """
                                {
                                    "id": 7,
                                    "email": "ana@example.com"
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        UserIdentityPort.AuthenticatedUser user =
                adapter.authenticate("ana@example.com", "SenhaTeste123!");

        assertEquals(7L, user.id());
        assertEquals("ana@example.com", user.email());
        server.verify();
    }

    @Test
    void shouldPropagateUnauthorizedResponseFromMsUsers() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(restClientBuilder).build();

        MsUsersIdentityAdapter adapter =
                new MsUsersIdentityAdapter(restClientBuilder, "http://localhost:8080");

        server.expect(requestTo("http://localhost:8080/users/me"))
                .andExpect(method(GET))
                .andRespond(withUnauthorizedRequest());

        assertThrows(
                HttpClientErrorException.Unauthorized.class,
                () -> adapter.authenticate("ana@example.com", "SenhaErrada123!")
        );

        server.verify();
    }

    @Test
    void shouldPropagateServerErrorFromMsUsers() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(restClientBuilder).build();

        MsUsersIdentityAdapter adapter =
                new MsUsersIdentityAdapter(restClientBuilder, "http://localhost:8080");

        server.expect(requestTo("http://localhost:8080/users/me"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        assertThrows(
                HttpServerErrorException.InternalServerError.class,
                () -> adapter.authenticate("ana@example.com", "SenhaTeste123!")
        );

        server.verify();
    }

    @Test
    void shouldRejectInvalidIdentityResponseFromMsUsers() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(restClientBuilder).build();

        MsUsersIdentityAdapter adapter =
                new MsUsersIdentityAdapter(restClientBuilder, "http://localhost:8080");

        server.expect(requestTo("http://localhost:8080/users/me"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(
                AuthenticationServiceException.class,
                () -> adapter.authenticate("ana@example.com", "SenhaTeste123!")
        );

        server.verify();
    }

}