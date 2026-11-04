package br.com.beca.financeira.transactions.application.port.out;

public interface UserIdentityPort {

    AuthenticatedUser authenticate(String email, String rawPassword);

    record AuthenticatedUser(Long id, String email){

    }
}
