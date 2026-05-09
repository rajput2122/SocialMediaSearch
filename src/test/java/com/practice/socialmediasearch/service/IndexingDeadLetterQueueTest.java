package com.practice.socialmediasearch.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IndexingDeadLetterQueueTest {

    @Test
    void recordsFailedIndexingAttempt() {
        IndexingDeadLetterQueue dlq = new IndexingDeadLetterQueue();
        RuntimeException cause = new RuntimeException("ES down");

        dlq.record("Location", 42L, cause);

        List<IndexingDeadLetterQueue.Entry> entries = dlq.snapshot();
        assertThat(entries).hasSize(1);
        IndexingDeadLetterQueue.Entry entry = entries.get(0);
        assertThat(entry.entityType()).isEqualTo("Location");
        assertThat(entry.entityId()).isEqualTo(42L);
        assertThat(entry.cause()).isSameAs(cause);
        assertThat(entry.timestamp()).isNotNull();
        assertThat(dlq.size()).isEqualTo(1);
    }

    @Test
    void clearRemovesAllEntries() {
        IndexingDeadLetterQueue dlq = new IndexingDeadLetterQueue();
        dlq.record("Tag", 1L, new RuntimeException("x"));
        dlq.record("Tag", 2L, new RuntimeException("y"));
        assertThat(dlq.size()).isEqualTo(2);

        dlq.clear();

        assertThat(dlq.size()).isZero();
        assertThat(dlq.snapshot()).isEmpty();
    }

    @Test
    void snapshotReturnsImmutableCopy() {
        IndexingDeadLetterQueue dlq = new IndexingDeadLetterQueue();
        dlq.record("User", 7L, new RuntimeException("boom"));

        List<IndexingDeadLetterQueue.Entry> snap = dlq.snapshot();

        assertThat(snap).hasSize(1);
        // mutating the snapshot must not affect the DLQ
        org.assertj.core.api.Assertions.assertThatThrownBy(snap::clear)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(dlq.size()).isEqualTo(1);
    }
}
