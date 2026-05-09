package com.practice.socialmediasearch.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory dead-letter queue for indexing events that fail to be written
 * to Elasticsearch after the H2 commit.
 *
 * In production this would be backed by a durable store (Redis, DB table, or
 * the message broker's built-in DLQ topic) and drained by an ops process. The
 * in-memory implementation keeps the contract observable for tests and the
 * /actuator surface without pulling in extra infra.
 */
@Component
@Slf4j
public class IndexingDeadLetterQueue {

    private final Queue<Entry> entries = new ConcurrentLinkedQueue<>();

    public void record(String entityType, Object entityId, Throwable cause) {
        Entry entry = new Entry(entityType, entityId, cause, Instant.now());
        entries.add(entry);
        log.error("Indexing failed — sent to DLQ. entity={} id={} cause={}",
                entityType, entityId, cause.toString());
    }

    public List<Entry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(entries));
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }

    public record Entry(String entityType, Object entityId, Throwable cause, Instant timestamp) {
    }
}
