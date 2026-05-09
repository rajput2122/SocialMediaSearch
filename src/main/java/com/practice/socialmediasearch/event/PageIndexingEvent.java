package com.practice.socialmediasearch.event;

import com.practice.socialmediasearch.model.Page;

public record PageIndexingEvent(Page page) implements IndexingEvent {

    @Override
    public String entityType() {
        return "Page";
    }

    @Override
    public Long entityId() {
        return page != null ? page.getPageId() : null;
    }
}
