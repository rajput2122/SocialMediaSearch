package com.practice.socialmediasearch.event;

import com.practice.socialmediasearch.model.Tag;

public record TagIndexingEvent(Tag tag) implements IndexingEvent {

    @Override
    public String entityType() {
        return "Tag";
    }

    @Override
    public Long entityId() {
        return tag != null ? tag.getTagId() : null;
    }
}
