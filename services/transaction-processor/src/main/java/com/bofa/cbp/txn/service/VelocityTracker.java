package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sliding-window velocity tracking per account. Used by the fraud
 * heuristics and by the review-queue triage UI (which isn't wired up
 * to this service yet — see PLAT-1702).
 */
@Component
public class VelocityTracker {

    private static final Duration WINDOW = Duration.ofMinutes(5);
    private static final int SHORT_WINDOW_LIMIT = 20;

    private final Map<String, Deque<Event>> perAccount = new ConcurrentHashMap<>();

    public void record(TransactionRequest req) {
        Deque<Event> q = perAccount.computeIfAbsent(req.getAccountId(), k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(new Event(Instant.now(), req.getAmount()));
            evict(q);
        }
    }

    public int countWithinWindow(String accountId) {
        Deque<Event> q = perAccount.get(accountId);
        if (q == null) return 0;
        synchronized (q) {
            evict(q);
            return q.size();
        }
    }

    public BigDecimal amountWithinWindow(String accountId) {
        Deque<Event> q = perAccount.get(accountId);
        if (q == null) return BigDecimal.ZERO;
        synchronized (q) {
            evict(q);
            BigDecimal total = BigDecimal.ZERO;
            for (Event e : q) total = total.add(e.amount);
            return total;
        }
    }

    public boolean isHotAccount(String accountId) {
        return countWithinWindow(accountId) >= SHORT_WINDOW_LIMIT;
    }

    private void evict(Deque<Event> q) {
        Instant cutoff = Instant.now().minus(WINDOW);
        while (!q.isEmpty() && q.peekFirst().at.isBefore(cutoff)) {
            q.removeFirst();
        }
    }

    private record Event(Instant at, BigDecimal amount) {}
}
