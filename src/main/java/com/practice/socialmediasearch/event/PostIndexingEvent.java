package com.practice.socialmediasearch.event;

import com.practice.socialmediasearch.model.Post;

public record PostIndexingEvent(Post post) implements IndexingEvent {

    @Override
    public String entityType() {
        return "Post";
    }

    @Override
    public Long entityId() {
        return post != null ? post.getPostId() : null;
    }
}
