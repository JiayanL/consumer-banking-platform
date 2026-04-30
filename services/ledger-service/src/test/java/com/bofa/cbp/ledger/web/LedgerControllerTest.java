package com.bofa.cbp.ledger.web;

import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.service.LedgerReportService;
import com.bofa.cbp.ledger.service.LedgerService;
import com.bofa.cbp.ledger.service.UnbalancedJournalException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({LedgerController.class, LedgerReportController.class})
class LedgerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LedgerService ledgerService;

    @MockBean
    private LedgerReportService reportService;

    // --- LedgerController: POST /journal ---

    @Test
    void submitJournal_balanced_returns201() throws Exception {
        Posting saved1 = new Posting("acct-A", new BigDecimal("100.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Posting saved2 = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("100.00"), "txn-1", Instant.now());
        when(ledgerService.submitJournal(any())).thenReturn(List.of(saved1, saved2));

        mockMvc.perform(post("/journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "txnId": "txn-1",
                                "postings": [
                                    {"accountId": "acct-A", "debit": 100.00, "credit": 0},
                                    {"accountId": "acct-B", "debit": 0, "credit": 100.00}
                                ]
                            }
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void submitJournal_unbalanced_returns422() throws Exception {
        when(ledgerService.submitJournal(any()))
                .thenThrow(new UnbalancedJournalException("txn-bad", new BigDecimal("100"), new BigDecimal("50")));

        mockMvc.perform(post("/journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "txnId": "txn-bad",
                                "postings": [
                                    {"accountId": "acct-A", "debit": 100.00, "credit": 0},
                                    {"accountId": "acct-B", "debit": 0, "credit": 50.00}
                                ]
                            }
                            """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void submitJournal_nullDebitCredit_defaultsToZero() throws Exception {
        Posting saved = new Posting("acct-A", BigDecimal.ZERO, BigDecimal.ZERO, "txn-1", Instant.now());
        when(ledgerService.submitJournal(any())).thenReturn(List.of(saved));

        mockMvc.perform(post("/journal")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "txnId": "txn-1",
                                "postings": [
                                    {"accountId": "acct-A"}
                                ]
                            }
                            """))
                .andExpect(status().isCreated());
    }

    // --- LedgerController: GET /postings ---

    @Test
    void byAccount_returnsPostings() throws Exception {
        Posting p = new Posting("acct-X", new BigDecimal("50.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        when(ledgerService.postingsForAccount("acct-X")).thenReturn(List.of(p));

        mockMvc.perform(get("/postings").param("accountId", "acct-X"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].accountId").value("acct-X"));
    }

    // --- LedgerController: GET /accounts/{id}/balance ---

    @Test
    void balance_returnsAccountBalance() throws Exception {
        when(ledgerService.deriveBalance("acct-Y")).thenReturn(new BigDecimal("750.00"));

        mockMvc.perform(get("/accounts/acct-Y/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acct-Y"))
                .andExpect(jsonPath("$.balance").value(750.00));
    }

    // --- LedgerReportController: GET /reports/net-movement ---

    @Test
    void netMovement_returnsPerAccountNetMovement() throws Exception {
        when(reportService.netMovementByAccount())
                .thenReturn(Map.of("acct-A", new BigDecimal("-100.00"), "acct-B", new BigDecimal("100.00")));

        mockMvc.perform(get("/reports/net-movement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acct-A").value(-100.00))
                .andExpect(jsonPath("$.acct-B").value(100.00));
    }

    // --- LedgerReportController: GET /reports/latest-activity ---

    @Test
    void latestActivity_returnsTimestamp() throws Exception {
        when(reportService.latestActivity()).thenReturn(Instant.parse("2026-06-15T12:00:00Z"));

        mockMvc.perform(get("/reports/latest-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latest").value("2026-06-15T12:00:00Z"));
    }

    @Test
    void latestActivity_noPostings_returnsNull() throws Exception {
        when(reportService.latestActivity()).thenReturn(null);

        mockMvc.perform(get("/reports/latest-activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latest").isEmpty());
    }

    // --- LedgerReportController: GET /reports/accounts/{id}/summary ---

    @Test
    void accountSummary_returnsAccountDetails() throws Exception {
        when(reportService.postingCount("acct-Z")).thenReturn(5L);
        when(reportService.totalDebitVolume("acct-Z")).thenReturn(new BigDecimal("300.00"));
        when(reportService.totalCreditVolume("acct-Z")).thenReturn(new BigDecimal("500.00"));

        mockMvc.perform(get("/reports/accounts/acct-Z/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value("acct-Z"))
                .andExpect(jsonPath("$.postingCount").value(5))
                .andExpect(jsonPath("$.totalDebit").value(300.00))
                .andExpect(jsonPath("$.totalCredit").value(500.00));
    }
}
