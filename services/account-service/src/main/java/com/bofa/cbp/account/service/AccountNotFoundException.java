package com.bofa.cbp.account.service;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long id) {
        super("account not found: " + id);
    }
}
