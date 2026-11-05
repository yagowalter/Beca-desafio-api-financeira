package br.com.beca.financeira.transactions.adapters.in.web.dto;

import br.com.beca.financeira.transactions.adapters.in.web.validation.ValidTransactionRequest;
import br.com.beca.financeira.transactions.domain.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


import java.math.BigDecimal;

@ValidTransactionRequest
public class CreateTransactionRequest {

    @Schema(description = "Descrição da transação", example = "Compra no mercado")
    @NotBlank(message = "Descrição é obrigatória")
    private String description;

    @Schema(description = "Valor em reais", example = "25.00")
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01" , message = "Valor deve ser maior do que zero")
    private BigDecimal amount;

    @Schema(description = "Tipo da transação", example = "PURCHASE")
    @NotNull(message = "Tipo da transação é obrigatório")
    private TransactionType type;

    @Schema(description = "Categoria, obrigatória para compras", example = "Alimentação")
    private String category;

    @Schema(description = "ID do usuário de destino, obrigatório para transferências", example = "47")
    private Long destinationUserId;

    public CreateTransactionRequest() {
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getDestinationUserId() {
        return destinationUserId;
    }

    public void setDestinationUserId(Long destinationUserId) {
        this.destinationUserId = destinationUserId;
    }
}