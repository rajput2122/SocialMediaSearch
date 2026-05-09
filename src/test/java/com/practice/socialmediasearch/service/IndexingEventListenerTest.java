package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.event.LocationIndexingEvent;
import com.practice.socialmediasearch.event.PageIndexingEvent;
import com.practice.socialmediasearch.event.PostIndexingEvent;
import com.practice.socialmediasearch.event.TagIndexingEvent;
import com.practice.socialmediasearch.event.UserIndexingEvent;
import com.practice.socialmediasearch.exception.ElasticsearchIndexingException;
import com.practice.socialmediasearch.model.Location;
import com.practice.socialmediasearch.model.Page;
import com.practice.socialmediasearch.model.Post;
import com.practice.socialmediasearch.model.Tag;
import com.practice.socialmediasearch.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IndexingEventListenerTest {

    private IndexingService indexingService;
    private IndexingDeadLetterQueue dlq;
    private IndexingEventListener listener;

    @BeforeEach
    void setUp() {
        indexingService = mock(IndexingService.class);
        dlq = new IndexingDeadLetterQueue();
        listener = new IndexingEventListener(indexingService, dlq);
    }

    // ── Happy paths — listener delegates to IndexingService ───────────────────

    @Test
    void onLocationIndexesLocation() {
        Location loc = Location.builder().locationId(1L).displayName("Bengaluru").build();

        listener.onLocation(new LocationIndexingEvent(loc));

        verify(indexingService).indexLocation(loc);
        assertThat(dlq.size()).isZero();
    }

    @Test
    void onTagIndexesTag() {
        Tag tag = Tag.builder().tagId(2L).tagName("java").build();

        listener.onTag(new TagIndexingEvent(tag));

        verify(indexingService).indexTag(tag);
        assertThat(dlq.size()).isZero();
    }

    @Test
    void onUserIndexesUser() {
        User user = User.builder().userId(3L).name("Atul").username("atulk").build();

        listener.onUser(new UserIndexingEvent(user));

        verify(indexingService).indexUser(user);
        assertThat(dlq.size()).isZero();
    }

    @Test
    void onPostIndexesPost() {
        Post post = Post.builder().postId(4L).caption("hi").build();

        listener.onPost(new PostIndexingEvent(post));

        verify(indexingService).indexPost(post);
        assertThat(dlq.size()).isZero();
    }

    @Test
    void onPageIndexesPage() {
        Page page = Page.builder().pageId(5L).pageName("Java Community").build();

        listener.onPage(new PageIndexingEvent(page));

        verify(indexingService).indexPage(page);
        assertThat(dlq.size()).isZero();
    }

    // ── Failure paths — failures route to DLQ, do not bubble ──────────────────

    @Test
    void onLocationRoutesToDlqWhenIndexingFails() {
        Location loc = Location.builder().locationId(1L).displayName("X").build();
        ElasticsearchIndexingException cause =
                new ElasticsearchIndexingException("Location", 1L, new RuntimeException("boom"));
        doThrow(cause).when(indexingService).indexLocation(any());

        listener.onLocation(new LocationIndexingEvent(loc));

        assertThat(dlq.size()).isEqualTo(1);
        IndexingDeadLetterQueue.Entry entry = dlq.snapshot().get(0);
        assertThat(entry.entityType()).isEqualTo("Location");
        assertThat(entry.entityId()).isEqualTo(1L);
        assertThat(entry.cause()).isSameAs(cause);
    }

    @Test
    void onTagRoutesToDlqWhenIndexingFails() {
        Tag tag = Tag.builder().tagId(2L).tagName("java").build();
        doThrow(new RuntimeException("boom")).when(indexingService).indexTag(any());

        listener.onTag(new TagIndexingEvent(tag));

        assertThat(dlq.snapshot()).singleElement().satisfies(e -> {
            assertThat(e.entityType()).isEqualTo("Tag");
            assertThat(e.entityId()).isEqualTo(2L);
        });
    }

    @Test
    void onUserRoutesToDlqWhenIndexingFails() {
        User user = User.builder().userId(3L).build();
        doThrow(new RuntimeException("boom")).when(indexingService).indexUser(any());

        listener.onUser(new UserIndexingEvent(user));

        assertThat(dlq.snapshot()).singleElement().satisfies(e -> {
            assertThat(e.entityType()).isEqualTo("User");
            assertThat(e.entityId()).isEqualTo(3L);
        });
    }

    @Test
    void onPostRoutesToDlqWhenIndexingFails() {
        Post post = Post.builder().postId(4L).build();
        doThrow(new RuntimeException("boom")).when(indexingService).indexPost(any());

        listener.onPost(new PostIndexingEvent(post));

        assertThat(dlq.snapshot()).singleElement().satisfies(e -> {
            assertThat(e.entityType()).isEqualTo("Post");
            assertThat(e.entityId()).isEqualTo(4L);
        });
    }

    @Test
    void onPageRoutesToDlqWhenIndexingFails() {
        Page page = Page.builder().pageId(5L).build();
        doThrow(new RuntimeException("boom")).when(indexingService).indexPage(any());

        listener.onPage(new PageIndexingEvent(page));

        assertThat(dlq.snapshot()).singleElement().satisfies(e -> {
            assertThat(e.entityType()).isEqualTo("Page");
            assertThat(e.entityId()).isEqualTo(5L);
        });
    }
}
