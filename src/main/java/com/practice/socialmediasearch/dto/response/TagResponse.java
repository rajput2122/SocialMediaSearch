package com.practice.socialmediasearch.dto.response;

import com.practice.socialmediasearch.model.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TagResponse {

    private Long tagId;
    private String tagName;

    public static TagResponse from(Tag tag) {
        return TagResponse.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .build();
    }
}

