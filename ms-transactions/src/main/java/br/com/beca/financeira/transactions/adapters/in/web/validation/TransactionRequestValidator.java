package br.com.beca.financeira.transactions.adapters.in.web.validation;

import br.com.beca.financeira.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.financeira.transactions.domain.TransactionType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionRequestValidator
        implements ConstraintValidator<ValidTransactionRequest, CreateTransactionRequest> {

    @Override
    public boolean isValid(
            CreateTransactionRequest request,
            ConstraintValidatorContext context
    ) {
        if (request == null || request.getType() == null) {
            return true;
        }

        boolean categoryRequired =
                request.getType() == TransactionType.PURCHASE
                        || request.getType() == TransactionType.TRANSFER;

        boolean categoryMissing =
                categoryRequired
                        && (request.getCategory() == null
                        || request.getCategory().isBlank());

        boolean destinationMissing =
                request.getType() == TransactionType.TRANSFER
                        && (request.getDestinationUserId() == null
                        || request.getDestinationUserId() <= 0);

        if (!categoryMissing && !destinationMissing) {
            return true;
        }

        context.disableDefaultConstraintViolation();

        if (categoryMissing) {
            String message = request.getType() == TransactionType.PURCHASE
                    ? "Categoria é obrigatória para compras"
                    : "Categoria é obrigatória para transferências";

            context.buildConstraintViolationWithTemplate(message)
                    .addPropertyNode("category")
                    .addConstraintViolation();
        }

        if (destinationMissing) {
            context.buildConstraintViolationWithTemplate(
                            "Usuário de destino é obrigatório para transferências"
                    )
                    .addPropertyNode("destinationUserId")
                    .addConstraintViolation();
        }

        return false;
    }
}
