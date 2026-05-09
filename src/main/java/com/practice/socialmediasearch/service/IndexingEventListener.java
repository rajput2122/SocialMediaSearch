package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.event.IndexingEvent;
import com.practice.socialmediasearch.event.LocationIndexingEvent;
import com.practice.socialmediasearch.event.PageIndexingEvent;
import com.practice.socialmediasearch.event.PostIndexingEvent;
import com.practice.socialmediasearch.event.TagIndexingEvent;
import com.practice.socialmediasearch.event.UserIndexingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Consumer;

/**
 * Consumes indexing events asynchronously after the writer's H2 transaction
 * commits, then drives the actual Elasticsearch write via {@link IndexingService}.
 * Failures are routed to {@link IndexingDeadLetterQueue} so a flaky/down ES
 * cluster never breaks the write path.
 */
@Component
@RequiredArgsConstructor
public class IndexingEventListener {

    private final IndexingService indexingService;
    private final IndexingDeadLetterQueue deadLetterQueue;

    @Async("indexingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLocation(LocationIndexingEvent event) {
        runOrDeadLetter(event, e -> indexingService.indexLocation(e.location()));
    }

    @Async("indexingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTag(TagIndexingEvent event) {
        runOrDeadLetter(event, e -> indexingService.indexTag(e.tag()));
    }

    @Async("indexingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUser(UserIndexingEvent event) {
        runOrDeadLetter(event, e -> indexingService.indexUser(e.user()));
    }

    @Async("indexingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPost(PostIndexingEvent event) {
        runOrDeadLetter(event, e -> indexingService.indexPost(e.post()));
    }

    @Async("indexingExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPage(PageIndexingEvent event) {
        runOrDeadLetter(event, e -> indexingService.indexPage(e.page()));
    }

    private <E extends IndexingEvent> void runOrDeadLetter(E event, Consumer<E> work) {
        try {
            work.accept(event);
        } catch (Exception ex) {
            deadLetterQueue.record(event.entityType(), event.entityId(), ex);
        }
    }
}
