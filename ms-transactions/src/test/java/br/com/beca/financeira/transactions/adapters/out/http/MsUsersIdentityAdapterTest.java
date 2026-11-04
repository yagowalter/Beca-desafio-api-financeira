package br.com.beca.financeira.transactions.adapters.out.http;

import br.com.beca.financeira.transactions.application.port.out.UserIdentityPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

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
}