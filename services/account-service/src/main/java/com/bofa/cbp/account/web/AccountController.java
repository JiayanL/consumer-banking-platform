package com.bofa.cbp.account.web;

import com.bofa.cbp.account.domain.Account;
import com.bofa.cbp.account.domain.AccountType;
import com.bofa.cbp.account.service.AccountNotFoundException;
import com.bofa.cbp.account.service.AccountService;
import com.bofa.cbp.auth.JwtValidator;
import com.bofa.cbp.auth.compliance.ComplianceCategory;
import com.bofa.cbp.auth.compliance.ComplianceCritical;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// TODO: add tests for freeze/unfreeze flows once audit log assertions are in place.
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;
    private final JwtValidator jwtValidator;

    @PostMapping
    public ResponseEntity<Account> create(@RequestBody CreateAccountRequest req) {
        Account created = service.createAccount(req.customerId(), req.type());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{id}")
    public Account getById(@PathVariable Long id) {
        return service.getAccount(id);
    }

    @GetMapping
    public List<Account> listByCustomer(@RequestParam String customerId) {
        return service.findByCustomer(customerId);
    }

    @PostMapping("/{id}/freeze")
    public Account freeze(@PathVariable Long id) {
        return service.freeze(id);
    }

    @PostMapping("/{id}/unfreeze")
    public Account unfreeze(@PathVariable Long id) {
        return service.unfreeze(id);
    }

    @GetMapping("/{id}/balance")
    public BalanceResponse balance(@PathVariable Long id) {
        BigDecimal amount = service.getBalance(id);
        return new BalanceResponse(id, amount);
    }

    @ComplianceCritical(
        category = ComplianceCategory.AUTHENTICATION,
        note = "Validates caller JWT before returning any account data."
    )
    @GetMapping("/whoami")
    public Map<String, Object> whoami(@RequestHeader("Authorization") String authorization) {
        String token = authorization == null ? "" : authorization.replaceFirst("(?i)^Bearer\\s+", "");
        JwtValidator.ValidationResult result = jwtValidator.validate(token);
        if (!result.isValid()) {
            return Map.of("authenticated", false, "reason", result.reason());
        }
        return Map.of("authenticated", true, "subject", result.subject());
    }

    // TODO: add tests for listByCustomer once seed fixtures are wired.

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
    }

    public record CreateAccountRequest(String customerId, AccountType type) {}

    public record BalanceResponse(Long id, BigDecimal balance) {}
}
