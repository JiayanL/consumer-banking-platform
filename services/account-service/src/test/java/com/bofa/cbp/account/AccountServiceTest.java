package com.bofa.cbp.account;

import com.bofa.cbp.account.domain.Account;
import com.bofa.cbp.account.domain.AccountStatus;
import com.bofa.cbp.account.domain.AccountType;
import com.bofa.cbp.account.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AccountServiceTest {

    @Autowired
    private AccountService accountService;

    @Test
    void createsAccountAndReturnsZeroBalance() {
        Account created = accountService.createAccount("cust-123", AccountType.CHECKING);
        assertNotNull(created.getId());
        assertEquals(AccountStatus.ACTIVE, created.getStatus());
        assertEquals(0, BigDecimal.ZERO.compareTo(created.getBalance()));

        BigDecimal balance = accountService.getBalance(created.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(balance));
    }
}
