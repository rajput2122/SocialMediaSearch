package com.practice.socialmediasearch.event;

import com.practice.socialmediasearch.model.Location;

public record LocationIndexingEvent(Location location) implements IndexingEvent {

    @Override
    public String entityType() {
        return "Location";
    }

    @Override
    public Long entityId() {
        return location != null ? location.getLocationId() : null;
    }
}
