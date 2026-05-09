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
public class SearchPage<T> {

    private List<T> content;
    private int size;
    private long totalHits;
    private String nextCursor;

    public static <T> SearchPage<T> empty(int size) {
        return SearchPage.<T>builder()
                .content(List.of())
                .size(size)
                .totalHits(0L)
                .nextCursor(null)
                .build();
    }
}
