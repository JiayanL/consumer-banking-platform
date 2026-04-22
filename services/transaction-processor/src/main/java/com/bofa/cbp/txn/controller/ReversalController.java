package com.bofa.cbp.txn.controller;

import com.bofa.cbp.txn.service.ReversalService;
import com.bofa.cbp.txn.service.ReversalService.ReversalResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/transactions")
public class ReversalController {

    private final ReversalService reversals;

    public ReversalController(ReversalService reversals) {
        this.reversals = reversals;
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<ReversalResult> reverse(@PathVariable Long id,
                                                   @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? "unspecified" : body.getOrDefault("reason", "unspecified");
        ReversalResult r = reversals.reverse(id, reason);
        return switch (r.status()) {
            case "OK"         -> ResponseEntity.ok(r);
            case "NOT_FOUND"  -> ResponseEntity.notFound().build();
            default           -> ResponseEntity.status(409).body(r);
        };
    }
}
