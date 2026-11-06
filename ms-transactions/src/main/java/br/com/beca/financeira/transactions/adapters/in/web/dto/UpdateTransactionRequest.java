package br.com.beca.financeira.transactions.adapters.in.web.dto;

import br.com.beca.financeira.transactions.domain.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateTransactionRequest(

        @NotBlank
        @Size(max = 150)
        String description,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 17, fraction = 2 ,
        message = "Valor deve ter no máximo 17 dígitos inteiros e 2 casas decimais")
        BigDecimal amount,

        @NotNull
        TransactionType type
) {

}
