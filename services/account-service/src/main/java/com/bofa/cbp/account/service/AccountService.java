package com.bofa.cbp.account.service;

import com.bofa.cbp.account.domain.Account;
import com.bofa.cbp.account.domain.AccountRepository;
import com.bofa.cbp.account.domain.AccountStatus;
import com.bofa.cbp.account.domain.AccountType;
import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountRepository repository;

    public Account createAccount(String customerId, AccountType type) {
        Account account = Account.builder()
                .customerId(customerId)
                .accountNumber(generateAccountNumber())
                .type(type)
                .balance(BigDecimal.ZERO)
                .status(AccountStatus.ACTIVE)
                .build();
        return repository.save(account);
    }

    public Account getAccount(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    public List<Account> findByCustomer(String customerId) {
        return repository.findAllByCustomerId(customerId);
    }

    public BigDecimal getBalance(Long id) {
        return getAccount(id).getBalance();
    }

    public Account freeze(Long id) {
        Account account = getAccount(id);
        account.setStatus(AccountStatus.FROZEN);
        Account saved = repository.save(account);
        logFreezeEvent(saved, "FREEZE");
        return saved;
    }

    public Account unfreeze(Long id) {
        Account account = getAccount(id);
        account.setStatus(AccountStatus.ACTIVE);
        Account saved = repository.save(account);
        logFreezeEvent(saved, "UNFREEZE");
        return saved;
    }

    @ComplianceCritical(
        category = ComplianceCategory.AUDIT_TRAIL,
        note = "Account freeze/unfreeze produces a compliance audit record."
    )
    void logFreezeEvent(Account account, String action) {
        log.info("audit: account_id={} customer_id={} action={} status={}",
                account.getId(), account.getCustomerId(), action, account.getStatus());
    }

    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }
}
