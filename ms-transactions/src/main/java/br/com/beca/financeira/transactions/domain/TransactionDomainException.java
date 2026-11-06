package br.com.beca.financeira.transactions.domain;

public class TransactionDomainException extends IllegalArgumentException {

    public TransactionDomainException(String message) {
        super(message);
    }
}
