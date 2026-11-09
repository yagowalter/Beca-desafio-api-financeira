package br.com.beca.financeira.transactions.adapters.in.web;


import br.com.beca.financeira.transactions.adapters.in.web.dto.CreateTransactionRequest;
import br.com.beca.financeira.transactions.application.port.in.AccountLimitsUseCase;
import br.com.beca.financeira.transactions.application.port.in.ExpenseAnalysisUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionCsvExportUseCase;
import br.com.beca.financeira.transactions.application.port.in.TransactionUseCase;
import br.com.beca.financeira.transactions.domain.ExpenseAnalysis;
import br.com.beca.financeira.transactions.domain.Transaction;
import br.com.beca.financeira.transactions.domain.TransactionType;
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
import java.time.LocalDate;
import java.util.List;


@Tag(name = "Transações", description = "Criação, consulta e análise de transações")
@SecurityRequirement(name = "basicAuth")
@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;
    private final ExpenseAnalysisUseCase expenseAnalysisUseCase;
    private final TransactionCsvExportUseCase transactionCsvExportUseCase;
    private final AccountLimitsUseCase accountLimitsUseCase;

    public TransactionController(
            TransactionUseCase transactionUseCase,
            ExpenseAnalysisUseCase expenseAnalysisUseCase,
            TransactionCsvExportUseCase transactionCsvExportUseCase,
            AccountLimitsUseCase accountLimitsUseCase
    ) {
        this.transactionUseCase = transactionUseCase;
        this.expenseAnalysisUseCase = expenseAnalysisUseCase;
        this.transactionCsvExportUseCase = transactionCsvExportUseCase;
        this.accountLimitsUseCase = accountLimitsUseCase;
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
            description = "Retorna as transações do usuário autenticado. "
                    + "Os filtros startDate e endDate devem ser informados juntos. "
                    + "O filtro type pode ser usado sozinho ou combinado com o período."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de transações retornada"),
            @ApiResponse(responseCode = "400", description = "Filtros ou período inválidos")
    })
    @GetMapping
    public ResponseEntity<List<Transaction>> findAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Data inicial do período, no formato ISO (AAAA-MM-DD)",
                    example = "2026-11-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data final do período, no formato ISO (AAAA-MM-DD)",
                    example = "2026-11-30")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Tipo da transação", example = "PURCHASE")
            @RequestParam(required = false) TransactionType type
    ) {
        return ResponseEntity.ok(
                transactionUseCase.findAllByUserIdAndFilters(
                        principal.id(),
                        startDate,
                        endDate,
                        type
                )
        );
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
            @Parameter(description = "Data inicial do período, no formato ISO (AAAA-MM-DD)",
                    example = "2026-11-01")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Data final do período, no formato ISO (AAAA-MM-DD)",
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
            description = "Baixa as transações do usuário autenticado no período informado. As datas devem estar no formato ISO (AAAA-MM-DD)."
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
            @Parameter(description = "Data inicial do período, no formato ISO (AAAA-MM-DD)",
                    example = "2026-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(description = "Data final do período, no formato ISO (AAAA-MM-DD)",
                    example = "2026-11-30")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate
    ) {
        byte[] csv = transactionCsvExportUseCase.exportCsv(
                principal.id(),
                startDate,
                endDate
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                new MediaType("text", "csv", StandardCharsets.UTF_8)
        );
        headers.set(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"transacoes.csv\""
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(csv);
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

    @Operation(
            summary = "Consultar saldo e limites da conta",
            description = "Retorna o saldo, limite diário e o limite por transação do usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo e limites retornados"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada"),
            @ApiResponse(responseCode = "503", description = "serviço de contas indisponível")
    })
    @GetMapping("/account-limits")
    public ResponseEntity<AccountLimitsUseCase.AccountLimitsView> getAccountLimits(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(
                accountLimitsUseCase.findForUser(principal.id())
        );
    }
}
