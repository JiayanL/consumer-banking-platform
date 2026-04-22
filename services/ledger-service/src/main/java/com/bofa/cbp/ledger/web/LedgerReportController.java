package com.bofa.cbp.ledger.web;

import com.bofa.cbp.ledger.service.LedgerReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Read-only reporting endpoints. Not currently covered by unit tests.
 */
@RestController
@RequestMapping("/reports")
public class LedgerReportController {

    @Autowired
    private LedgerReportService reportService;

    @GetMapping("/net-movement")
    public Map<String, BigDecimal> netMovement() {
        return reportService.netMovementByAccount();
    }

    @GetMapping("/latest-activity")
    public Map<String, Object> latestActivity() {
        Instant latest = reportService.latestActivity();
        Map<String, Object> out = new HashMap<>();
        out.put("latest", latest == null ? null : latest.toString());
        return out;
    }

    @GetMapping("/accounts/{id}/summary")
    public Map<String, Object> accountSummary(@PathVariable("id") String accountId) {
        Map<String, Object> out = new HashMap<>();
        out.put("accountId", accountId);
        out.put("postingCount", reportService.postingCount(accountId));
        out.put("totalDebit", reportService.totalDebitVolume(accountId));
        out.put("totalCredit", reportService.totalCreditVolume(accountId));
        return out;
    }
}
