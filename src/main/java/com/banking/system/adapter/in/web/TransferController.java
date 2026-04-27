package com.banking.system.adapter.in.web;

import com.banking.system.adapter.in.web.dto.TransferRequest;
import com.banking.system.adapter.in.web.dto.TransferResponse;
import com.banking.system.application.port.in.GetTransferUseCase;
import com.banking.system.application.port.in.TransferMoneyUseCase;
import com.banking.system.application.usecase.command.TransferMoneyCommand;
import com.banking.system.application.usecase.result.TransferResult;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.transfer.Transfer;
import com.banking.system.domain.transfer.TransferId;
import com.banking.system.infrastructure.annotation.WebAdapter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Currency;

@WebAdapter
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferMoneyUseCase transferMoneyUseCase;
    private final GetTransferUseCase   getTransferUseCase;

    /**
     * POST /api/v1/transfers
     * Inicia uma transferência de dinheiro. Idempotente – seguro para retentar com a mesma idempotencyKey.
     *
     * HTTP 201: transferência criada e concluída
     * HTTP 200: resposta idempotente (transferência já foi processada anteriormente)
     * HTTP 409: conflito de locking otimista – cliente deve retentar
     * HTTP 422: violação de regra de negócio (saldo insuficiente, conta bloqueada, etc.)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_AGENT', 'ADMIN')")
    public ResponseEntity<TransferResponse> transfer(
            @Valid @RequestBody TransferRequest request) {

        TransferMoneyCommand command = new TransferMoneyCommand(
                AccountId.of(request.sourceAccountId()),
                AccountId.of(request.targetAccountId()),
                Money.of(request.amount(), Currency.getInstance(request.currency())),
                request.idempotencyKey(),
                request.description());

        TransferResult resultado = transferMoneyUseCase.transfer(command);

        TransferResponse response = new TransferResponse(
                resultado.transferId().toString(),
                resultado.status().name(),
                resultado.idempotentResponse(),
                resultado.processedAt());

        // 200 para resposta idempotente (já processado), 201 para nova transferência
        HttpStatus status = resultado.idempotentResponse() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * GET /api/v1/transfers/{transferId}
     * Consulta o status e detalhes de uma transferência.
     */
    @GetMapping("/{transferId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_AGENT', 'ADMIN')")
    public ResponseEntity<TransferResponse> getTransfer(@PathVariable String transferId) {
        Transfer transferencia = getTransferUseCase.getTransfer(TransferId.of(transferId));
        return ResponseEntity.ok(new TransferResponse(
                transferencia.getId().toString(), transferencia.getStatus().name(),
                false, transferencia.getUpdatedAt()));
    }
}
