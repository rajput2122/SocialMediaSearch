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
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Fire-and-forget façade for write paths to enqueue an indexing job
 * without blocking on the Elasticsearch HTTP call.
 *
 * Events are delivered after the surrounding JPA transaction commits
 * (see {@link IndexingEventListener}). Callers must publish from inside
 * an active transaction so that the AFTER_COMMIT phase fires.
 */
@Component
@RequiredArgsConstructor
public class IndexingEventPublisher {

    private final ApplicationEventPublisher publisher;

    public void publishLocation(Location location) {
        publisher.publishEvent(new LocationIndexingEvent(location));
    }

    public void publishTag(Tag tag) {
        publisher.publishEvent(new TagIndexingEvent(tag));
    }

    public void publishUser(User user) {
        publisher.publishEvent(new UserIndexingEvent(user));
    }

    public void publishPost(Post post) {
        publisher.publishEvent(new PostIndexingEvent(post));
    }

    public void publishPage(Page page) {
        publisher.publishEvent(new PageIndexingEvent(page));
    }
}
