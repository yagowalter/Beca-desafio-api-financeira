package br.com.beca.financeira.users.application.port.out;

public interface PasswordHasherPort {
    String hash(String rawPassword);
}
