package br.com.beca.financeira.users.application.service;

public class InvalidCsvHeaderException extends RuntimeException {

    public InvalidCsvHeaderException(String message) {
        super(message);
    }
}
