package com.banking.system.adapter.in.web;

import com.banking.system.adapter.in.web.dto.*;
import com.banking.system.application.port.in.CreateAccountUseCase;
import com.banking.system.application.port.in.GetAccountBalanceUseCase;
import com.banking.system.application.usecase.command.CreateAccountCommand;
import com.banking.system.application.usecase.result.AccountCreatedResult;
import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.infrastructure.annotation.WebAdapter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@WebAdapter
@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;

    /**
     * POST /api/v1/accounts
     * Cria uma nova conta bancária.
     * Requer ROLE_BANK_AGENT ou ROLE_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('BANK_AGENT', 'ADMIN')")
    public ResponseEntity<CreateAccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        log.info("REST criarConta: titular={} moeda={}", request.ownerId(), request.currency());

        AccountCreatedResult resultado = createAccountUseCase.createAccount(
                CreateAccountCommand.of(request.ownerId(), request.currency()));

        CreateAccountResponse response = new CreateAccountResponse(
                resultado.accountId().toString(), resultado.ownerId(),
                resultado.currency().getCurrencyCode(), resultado.createdAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/accounts/{accountId}/balance
     * Retorna o saldo atual calculado a partir do ledger.
     * Requer ROLE_CUSTOMER (conta própria) ou ROLE_ADMIN.
     */
    @GetMapping("/{accountId}/balance")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'BANK_AGENT', 'ADMIN')")
    public ResponseEntity<AccountBalanceResponse> getBalance(@PathVariable String accountId) {
        Money saldo = getAccountBalanceUseCase.getBalance(AccountId.of(accountId));
        return ResponseEntity.ok(new AccountBalanceResponse(
                accountId, saldo.amount(), saldo.currency().getCurrencyCode()));
    }
}
