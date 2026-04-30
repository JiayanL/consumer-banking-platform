package com.bofa.cbp.txn.service;

import com.bofa.cbp.txn.domain.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricsRecorderTest {

    private MetricsRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new MetricsRecorder();
    }

    @Test
    void initialCountsAreZero() {
        assertEquals(0, recorder.count(TransactionStatus.ACCEPTED));
        assertEquals(0, recorder.count(TransactionStatus.REJECTED));
        assertEquals(0, recorder.count(TransactionStatus.FAILED));
        assertEquals(0, recorder.processed());
        assertEquals(0, recorder.rejected());
    }

    @Test
    void recordAccepted() {
        recorder.record(TransactionStatus.ACCEPTED);

        assertEquals(1, recorder.count(TransactionStatus.ACCEPTED));
        assertEquals(1, recorder.processed());
        assertEquals(0, recorder.rejected());
    }

    @Test
    void recordRejectedIncrementsRejected() {
        recorder.record(TransactionStatus.REJECTED);

        assertEquals(1, recorder.count(TransactionStatus.REJECTED));
        assertEquals(1, recorder.rejected());
    }

    @Test
    void recordFailedIncrementsRejected() {
        recorder.record(TransactionStatus.FAILED);

        assertEquals(1, recorder.count(TransactionStatus.FAILED));
        assertEquals(1, recorder.rejected());
    }

    @Test
    void rejectionRateCalculation() {
        recorder.record(TransactionStatus.ACCEPTED);
        recorder.record(TransactionStatus.ACCEPTED);
        recorder.record(TransactionStatus.ACCEPTED);
        recorder.record(TransactionStatus.REJECTED);

        assertEquals(0.25, recorder.rejectionRate(), 0.0001);
    }

    @Test
    void rejectionRateZeroWhenEmpty() {
        assertEquals(0.0, recorder.rejectionRate(), 0.0001);
    }

    @Test
    void resetClearsAll() {
        recorder.record(TransactionStatus.ACCEPTED);
        recorder.record(TransactionStatus.REJECTED);
        recorder.record(TransactionStatus.FAILED);

        recorder.reset();

        assertEquals(0, recorder.count(TransactionStatus.ACCEPTED));
        assertEquals(0, recorder.count(TransactionStatus.REJECTED));
        assertEquals(0, recorder.count(TransactionStatus.FAILED));
        assertEquals(0, recorder.processed());
        assertEquals(0, recorder.rejected());
    }
}
