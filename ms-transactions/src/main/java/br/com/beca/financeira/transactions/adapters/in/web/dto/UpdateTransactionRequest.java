package br.com.beca.financeira.transactions.adapters.in.web.dto;

import br.com.beca.financeira.transactions.domain.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateTransactionRequest(

        @NotBlank
        @Size(max = 150)
        String description,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        TransactionType type
) {

}
