package com.bofa.cbp.wire.service;

public class WireNotFoundException extends RuntimeException {
    public WireNotFoundException(Long id) {
        super("wire transfer not found: " + id);
    }
}
