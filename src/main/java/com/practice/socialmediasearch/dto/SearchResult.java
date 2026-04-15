package com.practice.socialmediasearch.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult {

    private String id;
    private SearchType type;
    private String primaryText;
    private String secondaryText;
    private String locationName;
    private List<String> tags;
}

