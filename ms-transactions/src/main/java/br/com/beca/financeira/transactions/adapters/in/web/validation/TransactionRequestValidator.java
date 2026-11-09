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

        boolean categoryMissing =
                request.getType() == TransactionType.PURCHASE
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
            context.buildConstraintViolationWithTemplate(
                            "Categoria é obrigatória para compras"
                    )
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
