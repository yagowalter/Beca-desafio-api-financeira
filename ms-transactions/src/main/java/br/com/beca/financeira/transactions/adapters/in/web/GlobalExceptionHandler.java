package br.com.beca.financeira.transactions.adapters.in.web;


import br.com.beca.financeira.transactions.application.port.service.AccountLimitsUnavailableException;
import br.com.beca.financeira.transactions.application.port.service.AccountNotFoundException;
import br.com.beca.financeira.transactions.application.port.service.TransactionNotFoundException;
import br.com.beca.financeira.transactions.domain.TransactionDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleNotFound(TransactionNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleAccountNotFound(AccountNotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
    }

    @ExceptionHandler(AccountLimitsUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ProblemDetail handleAccountLimitsUnavailable(AccountLimitsUnavailableException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Um ou mais campos estão inválidos"
        );

        List<Map<String, String>> erros = exception.getBindingResult()
                .getAllErrors()
                .stream()
                .map(error -> {
                    String campo = error instanceof FieldError fieldError
                            ? fieldError.getField()
                            : error.getObjectName();

                    String mensagem = error.getDefaultMessage() != null
                            ? error.getDefaultMessage()
                            : "Valor inválido";

                    return Map.of(
                            "campo", campo,
                            "message", mensagem
                    );
                })
                .toList();

        problem.setProperty("erros", erros);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidation(HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Corpo da requisição inválido ou com valores desconhecidos"
        );
    }

    @ExceptionHandler(TransactionDomainException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleDomainValidation(TransactionDomainException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage()
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida"
        );
    }
}
