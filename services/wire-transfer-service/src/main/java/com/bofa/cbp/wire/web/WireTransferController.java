package com.bofa.cbp.wire.web;

import com.bofa.cbp.wire.domain.WireStatus;
import com.bofa.cbp.wire.domain.WireTransfer;
import com.bofa.cbp.wire.service.WireNotFoundException;
import com.bofa.cbp.wire.service.WireTransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/wires")
public class WireTransferController {

    @Autowired
    private WireTransferService service;

    @PostMapping
    public ResponseEntity<WireTransfer> create(@RequestBody WireRequest req) {
        WireTransfer w = service.initiate(
                req.senderAccount,
                req.beneficiaryName,
                req.beneficiaryAccount,
                req.amount,
                req.currency
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(w);
    }

    @GetMapping("/{id}")
    public WireTransfer getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping
    public List<WireTransfer> listByStatus(@RequestParam WireStatus status) {
        return service.findByStatus(status);
    }

    @PostMapping("/{id}/cancel")
    public WireTransfer cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @ExceptionHandler(WireNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(WireNotFoundException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("error", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    public static class WireRequest {
        public String senderAccount;
        public String beneficiaryName;
        public String beneficiaryAccount;
        public BigDecimal amount;
        public String currency;
    }
}
