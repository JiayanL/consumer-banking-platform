package com.bofa.cbp.ledger.service;

import com.bofa.cbp.ledger.domain.Posting;
import com.bofa.cbp.ledger.domain.PostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerReportServiceTest {

    @Mock
    private PostingRepository repository;

    @InjectMocks
    private LedgerReportService reportService;

    // --- netMovementByAccount ---

    @Test
    void netMovementByAccount_computesPerAccountCreditMinusDebit() {
        Posting p1 = new Posting("acct-A", new BigDecimal("100.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Posting p2 = new Posting("acct-B", BigDecimal.ZERO, new BigDecimal("100.00"), "txn-1", Instant.now());
        Posting p3 = new Posting("acct-A", BigDecimal.ZERO, new BigDecimal("30.00"), "txn-2", Instant.now());
        when(repository.findAll()).thenReturn(List.of(p1, p2, p3));

        Map<String, BigDecimal> result = reportService.netMovementByAccount();

        assertThat(result).hasSize(2);
        assertThat(result.get("acct-A")).isEqualByComparingTo(new BigDecimal("-70.00"));
        assertThat(result.get("acct-B")).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    void netMovementByAccount_emptyRepository_returnsEmptyMap() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        Map<String, BigDecimal> result = reportService.netMovementByAccount();

        assertThat(result).isEmpty();
    }

    // --- latestActivity ---

    @Test
    void latestActivity_returnsLatestTimestamp() {
        Instant earlier = Instant.parse("2026-01-01T00:00:00Z");
        Instant later = Instant.parse("2026-06-15T12:00:00Z");
        Posting p1 = new Posting("acct-A", BigDecimal.ZERO, BigDecimal.ZERO, "txn-1", earlier);
        Posting p2 = new Posting("acct-B", BigDecimal.ZERO, BigDecimal.ZERO, "txn-2", later);
        when(repository.findAll()).thenReturn(List.of(p1, p2));

        Instant result = reportService.latestActivity();

        assertThat(result).isEqualTo(later);
    }

    @Test
    void latestActivity_emptyRepository_returnsNull() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        Instant result = reportService.latestActivity();

        assertThat(result).isNull();
    }

    @Test
    void latestActivity_skipsNullTimestamps() {
        Instant known = Instant.parse("2026-03-01T08:00:00Z");
        Posting withTs = new Posting("acct-A", BigDecimal.ZERO, BigDecimal.ZERO, "txn-1", known);
        Posting withoutTs = new Posting("acct-B", BigDecimal.ZERO, BigDecimal.ZERO, "txn-2", null);
        when(repository.findAll()).thenReturn(List.of(withTs, withoutTs));

        Instant result = reportService.latestActivity();

        assertThat(result).isEqualTo(known);
    }

    // --- postingCount ---

    @Test
    void postingCount_returnsNumberOfPostingsForAccount() {
        Posting p1 = new Posting("acct-X", BigDecimal.ZERO, BigDecimal.ZERO, "txn-1", Instant.now());
        Posting p2 = new Posting("acct-X", BigDecimal.ZERO, BigDecimal.ZERO, "txn-2", Instant.now());
        when(repository.findAllByAccountId("acct-X")).thenReturn(List.of(p1, p2));

        long count = reportService.postingCount("acct-X");

        assertThat(count).isEqualTo(2);
    }

    @Test
    void postingCount_noPostings_returnsZero() {
        when(repository.findAllByAccountId("acct-none")).thenReturn(Collections.emptyList());

        long count = reportService.postingCount("acct-none");

        assertThat(count).isZero();
    }

    // --- totalDebitVolume ---

    @Test
    void totalDebitVolume_sumsAllDebitsForAccount() {
        Posting p1 = new Posting("acct-D", new BigDecimal("100.00"), BigDecimal.ZERO, "txn-1", Instant.now());
        Posting p2 = new Posting("acct-D", new BigDecimal("250.50"), BigDecimal.ZERO, "txn-2", Instant.now());
        when(repository.findAllByAccountId("acct-D")).thenReturn(List.of(p1, p2));

        BigDecimal total = reportService.totalDebitVolume("acct-D");

        assertThat(total).isEqualByComparingTo(new BigDecimal("350.50"));
    }

    @Test
    void totalDebitVolume_noPostings_returnsZero() {
        when(repository.findAllByAccountId("acct-none")).thenReturn(Collections.emptyList());

        BigDecimal total = reportService.totalDebitVolume("acct-none");

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // --- totalCreditVolume ---

    @Test
    void totalCreditVolume_sumsAllCreditsForAccount() {
        Posting p1 = new Posting("acct-E", BigDecimal.ZERO, new BigDecimal("400.00"), "txn-1", Instant.now());
        Posting p2 = new Posting("acct-E", BigDecimal.ZERO, new BigDecimal("600.00"), "txn-2", Instant.now());
        when(repository.findAllByAccountId("acct-E")).thenReturn(List.of(p1, p2));

        BigDecimal total = reportService.totalCreditVolume("acct-E");

        assertThat(total).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    void totalCreditVolume_noPostings_returnsZero() {
        when(repository.findAllByAccountId("acct-none")).thenReturn(Collections.emptyList());

        BigDecimal total = reportService.totalCreditVolume("acct-none");

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
