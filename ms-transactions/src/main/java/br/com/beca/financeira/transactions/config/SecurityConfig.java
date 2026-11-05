package br.com.beca.financeira.transactions.config;

import br.com.beca.financeira.transactions.adapters.in.security.MsUsersAuthenticationProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public BasicAuthenticationEntryPoint basicAuthenticationEntryPoint() {
        BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
        entryPoint.setRealmName("ms-transactions");
        return entryPoint;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            MsUsersAuthenticationProvider authenticationProvider,
            BasicAuthenticationEntryPoint basicAuthenticationEntryPoint
    ) throws Exception {
        AuthenticationManager authenticationManager =
                new ProviderManager(authenticationProvider);

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(authenticationManager)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic ->
                        basic.authenticationEntryPoint((request, response, exception) -> {
                            if (exception instanceof AuthenticationServiceException) {
                                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                            } else {
                                basicAuthenticationEntryPoint.commence(request, response, exception);
                            }
                        }))
                .build();
    }
}
