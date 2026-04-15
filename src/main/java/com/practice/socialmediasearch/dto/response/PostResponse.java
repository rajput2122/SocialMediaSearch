package com.practice.socialmediasearch.dto.response;

import com.practice.socialmediasearch.model.Location;
import com.practice.socialmediasearch.model.Post;
import com.practice.socialmediasearch.model.Tag;
import com.practice.socialmediasearch.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {

    private Long postId;
    private Long userId;
    private String username;
    private String caption;
    private Long locationId;
    private String locationName;
    private List<Long> tagIds;
    private List<String> tagNames;

    public static PostResponse from(Post post) {
        User user = post.getUser();
        Location location = post.getLocation();
        List<Tag> tags = post.getTags() != null ? post.getTags() : Collections.emptyList();

        return PostResponse.builder()
                .postId(post.getPostId())
                .userId(user != null ? user.getUserId() : null)
                .username(user != null ? user.getUsername() : null)
                .caption(post.getCaption())
                .locationId(location != null ? location.getLocationId() : null)
                .locationName(location != null ? location.getDisplayName() : null)
                .tagIds(tags.stream().map(Tag::getTagId).toList())
                .tagNames(tags.stream().map(Tag::getTagName).toList())
                .build();
    }
}

