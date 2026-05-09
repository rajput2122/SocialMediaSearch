package com.practice.socialmediasearch.event;

import com.practice.socialmediasearch.model.User;

public record UserIndexingEvent(User user) implements IndexingEvent {

    @Override
    public String entityType() {
        return "User";
    }

    @Override
    public Long entityId() {
        return user != null ? user.getUserId() : null;
    }
}
