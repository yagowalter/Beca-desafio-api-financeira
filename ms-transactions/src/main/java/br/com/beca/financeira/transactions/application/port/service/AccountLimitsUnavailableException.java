package br.com.beca.financeira.transactions.application.port.service;

public class AccountLimitsUnavailableException extends RuntimeException {
    public AccountLimitsUnavailableException(String message) {
        super(message);
    }

    public AccountLimitsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
