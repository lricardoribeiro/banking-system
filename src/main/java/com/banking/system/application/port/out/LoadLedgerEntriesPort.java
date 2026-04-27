package com.banking.system.application.port.out;

import com.banking.system.domain.account.AccountId;
import com.banking.system.domain.account.Money;
import com.banking.system.domain.ledger.LedgerEntry;
import com.banking.system.domain.transfer.TransferId;
import java.util.List;

/** Porta Secundária: leitura de lançamentos do ledger e cálculo de saldo. */
public interface LoadLedgerEntriesPort {

    List<LedgerEntry> loadEntriesByAccount(AccountId accountId);

    List<LedgerEntry> loadEntriesByTransfer(TransferId transferId);

    /**
     * Calcula o saldo como: SUM(CRÉDITO) - SUM(DÉBITO) para a conta informada.
     * Este é o saldo autoritativo – não cacheado, calculado diretamente do ledger.
     *
     * Nota de concorrência: chamado dentro da mesma transação que as operações
     * de débito/crédito para garantir leitura consistente (isolamento REPEATABLE_READ).
     */
    Money calculateBalance(AccountId accountId);
}
