package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.event.LocationIndexingEvent;
import com.practice.socialmediasearch.event.PageIndexingEvent;
import com.practice.socialmediasearch.event.PostIndexingEvent;
import com.practice.socialmediasearch.event.TagIndexingEvent;
import com.practice.socialmediasearch.event.UserIndexingEvent;
import com.practice.socialmediasearch.model.Location;
import com.practice.socialmediasearch.model.Page;
import com.practice.socialmediasearch.model.Post;
import com.practice.socialmediasearch.model.Tag;
import com.practice.socialmediasearch.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class IndexingEventPublisherTest {

    private ApplicationEventPublisher springPublisher;
    private IndexingEventPublisher publisher;

    @BeforeEach
    void setUp() {
        springPublisher = mock(ApplicationEventPublisher.class);
        publisher = new IndexingEventPublisher(springPublisher);
    }

    @Test
    void publishLocationEmitsLocationIndexingEvent() {
        Location loc = Location.builder().locationId(1L).displayName("Bengaluru").build();

        publisher.publishLocation(loc);

        verify(springPublisher).publishEvent(new LocationIndexingEvent(loc));
    }

    @Test
    void publishTagEmitsTagIndexingEvent() {
        Tag tag = Tag.builder().tagId(2L).tagName("java").build();

        publisher.publishTag(tag);

        verify(springPublisher).publishEvent(new TagIndexingEvent(tag));
    }

    @Test
    void publishUserEmitsUserIndexingEvent() {
        User user = User.builder().userId(3L).name("Atul").username("atulk").build();

        publisher.publishUser(user);

        verify(springPublisher).publishEvent(new UserIndexingEvent(user));
    }

    @Test
    void publishPostEmitsPostIndexingEvent() {
        Post post = Post.builder().postId(4L).caption("hi").build();

        publisher.publishPost(post);

        verify(springPublisher).publishEvent(new PostIndexingEvent(post));
    }

    @Test
    void publishPageEmitsPageIndexingEvent() {
        Page page = Page.builder().pageId(5L).pageName("Java Community").build();

        publisher.publishPage(page);

        verify(springPublisher).publishEvent(new PageIndexingEvent(page));
    }
}
