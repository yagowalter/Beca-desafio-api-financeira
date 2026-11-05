package br.com.beca.financeira.transactions.adapters.in.web;


import br.com.beca.financeira.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.financeira.transactions.application.port.in.ExpenseAnalysisUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;
import br.com.beca.financeira.transactions.domain.Transaction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import br.com.beca.financeira.transactions.adapters.in.security.MsUsersAuthenticationProvider.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Tag(name = "Transações", description = "Criação, consulta e análise de transações")
@SecurityRequirement(name = "basicAuth")
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

    @Operation(
            summary = "Criar transação",
            description = "Cria uma transação para o usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transação criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
            @ApiResponse(responseCode = "503", description = "Serviço de contas indisponível")
    })
    @PostMapping
    public ResponseEntity<Transaction> create(
            @Valid @RequestBody CreateTransactionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Transaction transaction = transactionUseCase.create(
                principal.id(),
                request.getDescription(),
                request.getAmount(),
                request.getType(),
                request.getCategory(),
                request.getDestinationUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction);
    }

    @Operation(
            summary = "Listar transações do usuário autenticado",
            description = "Retorna somente as transações pertencentes ao usuário autenticado."
    )
    @ApiResponse(responseCode = "200", description = "Lista de transações retornada")
    @GetMapping
    public ResponseEntity<List<Transaction>> findAll(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(transactionUseCase.findAllByUserId(principal.id()));
    }

    @Operation(
            summary = "Analisar despesas",
            description = "Retorna a análise de despesas do usuário autenticado. Se as datas forem omitidas, considera o mês atual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Análise retornada"),
            @ApiResponse(responseCode = "400", description = "Período inválido")
    })
    @GetMapping("/analysis")
    public ResponseEntity<ExpenseAnalysis> analyzeExpenses(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Data inicial do período, no formato AAAA-MM-DD",
                    example = "2026-11-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data final do período, no formato AAAA-MM-DD",
                    example = "2026-11-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                expenseAnalysisUseCase.analyzeExpenses(
                        principal.id(),
                        startDate,
                        endDate
                )
        );
    }

    @Operation(
            summary = "Exportar transações em CSV",
            description = "Baixa as transações do usuário autenticado no período informado. As datas devem estar no formato AAAA-MM-DD."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Arquivo CSV gerado",
                    content = @Content(
                            mediaType = "text/csv",
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Período inválido")
    })
    @GetMapping(value = "/export", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Data inicial do período, no formato AAAA-MM-DD",
                    example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data final do período, no formato AAAA-MM-DD",
                    example = "2026-11-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<Transaction> transactions = transactionUseCase.findAllByUserIdAndPeriod(
                principal.id(),
                startDate,
                endDate
        );

        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("data;tipo;categoria;descricao;valor (R$);status;motivo_rejeicao\r\n");

        NumberFormat amountFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        amountFormat.setMinimumFractionDigits(2);
        amountFormat.setMaximumFractionDigits(2);

        for (Transaction transaction : transactions) {
            csv.append(csvField(transaction.
                            getOccurredAt().
                            toLocalDate().
                            format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    )).append(';')
                    .append(csvField(transaction.getType().name())).append(';')
                    .append(csvField(transaction.getCategory())).append(';')
                    .append(csvField(transaction.getDescription())).append(';')
                    .append(amountFormat.format(transaction.getAmount())).append(';')
                    .append(csvField(transaction.getStatus() == null
                            ? "" : transaction.getStatus().name())).append(';')
                    .append(csvField(transaction.getRejectionReason()))
                    .append("\r\n");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"transacoes.csv\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvField(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @Operation(
            summary = "Consultar transação por ID",
            description = "Retorna uma transação pertencente ao usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transação encontrada"),
            @ApiResponse(responseCode = "404", description = "Transação não encontrada")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Transaction> findById(
            @Parameter(description = "Identificador da transação", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                transactionUseCase.findByIdAndUserId(id, principal.id()));
    }
}
