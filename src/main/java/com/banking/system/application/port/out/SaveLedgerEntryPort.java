package com.banking.system.application.port.out;

import com.banking.system.domain.ledger.LedgerEntry;
import java.util.List;

/** Porta Secundária: insere lançamentos no ledger (apenas INSERT – nunca UPDATE ou DELETE). */
public interface SaveLedgerEntryPort {
    LedgerEntry saveLedgerEntry(LedgerEntry entry);
    List<LedgerEntry> saveLedgerEntries(List<LedgerEntry> entries);
}
