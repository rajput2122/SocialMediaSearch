package com.practice.socialmediasearch.event;

public sealed interface IndexingEvent
        permits LocationIndexingEvent, TagIndexingEvent, UserIndexingEvent,
                PostIndexingEvent, PageIndexingEvent {

    String entityType();

    Long entityId();
}
