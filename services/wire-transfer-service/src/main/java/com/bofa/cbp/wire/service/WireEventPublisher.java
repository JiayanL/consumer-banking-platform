package com.bofa.cbp.wire.service;

import com.bofa.cbp.wire.domain.WireTransfer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Stubbed event bus hook. In production, events are mirrored to the
 * payments Kafka topic; for now we just buffer them in memory so the
 * settlement batch job can replay them during the rollout window.
 * Not yet covered by tests.
 */
@Component
public class WireEventPublisher {

    private final List<String> buffered = new ArrayList<>();

    public synchronized void publishInitiated(WireTransfer wire) {
        buffered.add(format("INITIATED", wire));
    }

    public synchronized void publishStatusChange(WireTransfer wire, String previousStatus) {
        buffered.add(format("STATUS " + previousStatus + " -> " + wire.getStatus(), wire));
    }

    public synchronized void publishCancelled(WireTransfer wire, String reason) {
        buffered.add(format("CANCELLED (" + reason + ")", wire));
    }

    public synchronized List<String> drain() {
        List<String> snapshot = List.copyOf(buffered);
        buffered.clear();
        return snapshot;
    }

    public synchronized List<String> peek() {
        return Collections.unmodifiableList(new ArrayList<>(buffered));
    }

    private String format(String kind, WireTransfer wire) {
        return "[" + kind + "] ref=" + wire.getReferenceNumber()
                + " amount=" + wire.getAmount()
                + " " + wire.getCurrency();
    }
}
