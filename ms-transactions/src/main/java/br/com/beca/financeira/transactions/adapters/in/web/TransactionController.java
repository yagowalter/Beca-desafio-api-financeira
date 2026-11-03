package br.com.beca.financeira.transactions.adapters.in.web;


import br.com.beca.financeira.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.financeira.transactions.application.port.in.ExpenseAnalysisUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;
import br.com.beca.financeira.transactions.domain.Transaction;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;
    private final ExpenseAnalysisUseCase expenseAnalysisUseCase;

    public TransactionController(
            TransactionUseCase transactionUseCase,
            ExpenseAnalysisUseCase expenseAnalysisUseCase
    ) {
        this.transactionUseCase = transactionUseCase;
        this.expenseAnalysisUseCase = expenseAnalysisUseCase;
    }

    @PostMapping
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody CreateTransactionRequest request
    ) {
        Transaction transaction = transactionUseCase.create(
                request.getUserId(),
                request.getDescription(),
                request.getAmount(),
                request.getType(),
                request.getCategory(),
                request.getDestinationUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> findAll() {
        return ResponseEntity.ok(transactionUseCase.findAll());
    }

    @GetMapping("/analysis")
    public ResponseEntity<ExpenseAnalysis> analyzeExpenses(
            @RequestParam Long userId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                expenseAnalysisUseCase.analyzeExpenses(
                        userId,
                        startDate,
                        endDate
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> findById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionUseCase.findById(id));
    }


}
