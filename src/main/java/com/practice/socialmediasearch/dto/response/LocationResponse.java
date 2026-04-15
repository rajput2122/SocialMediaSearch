package com.practice.socialmediasearch.dto.response;

import com.practice.socialmediasearch.model.Location;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationResponse {

    private Long locationId;
    private String displayName;

    public static LocationResponse from(Location location) {
        return LocationResponse.builder()
                .locationId(location.getLocationId())
                .displayName(location.getDisplayName())
                .build();
    }
}

