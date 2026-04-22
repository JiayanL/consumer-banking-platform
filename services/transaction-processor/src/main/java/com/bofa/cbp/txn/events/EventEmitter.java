package com.bofa.cbp.txn.events;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-process event sink. In production this is a Kafka producer; for
 * the demo checkout we keep events in memory so tests can assert on
 * them without spinning up a broker.
 */
@Component
public class EventEmitter {

    private final List<TransactionEvent> emitted = new CopyOnWriteArrayList<>();

    public void emit(TransactionEvent event) {
        emitted.add(event);
    }

    public List<TransactionEvent> drain() {
        List<TransactionEvent> copy = List.copyOf(emitted);
        emitted.clear();
        return copy;
    }
}
