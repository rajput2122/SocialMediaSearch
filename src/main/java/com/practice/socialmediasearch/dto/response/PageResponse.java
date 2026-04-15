package com.practice.socialmediasearch.dto.response;

import com.practice.socialmediasearch.model.Location;
import com.practice.socialmediasearch.model.Page;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {

    private Long pageId;
    private String pageName;
    private String bio;
    private Long locationId;
    private String locationName;

    public static PageResponse from(Page page) {
        Location location = page.getLocation();

        return PageResponse.builder()
                .pageId(page.getPageId())
                .pageName(page.getPageName())
                .bio(page.getBio())
                .locationId(location != null ? location.getLocationId() : null)
                .locationName(location != null ? location.getDisplayName() : null)
                .build();
    }
}

