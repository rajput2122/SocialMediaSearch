package com.practice.socialmediasearch.controller;

import com.practice.socialmediasearch.common.ApiResponse;
import com.practice.socialmediasearch.dto.SearchPage;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import com.practice.socialmediasearch.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Validated
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<ApiResponse<SearchPage<SearchResult>>> search(
            @RequestParam("q") @NotBlank(message = "q is required") String query,
            @RequestParam SearchType type,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be >= 1")
                    @Max(value = 100, message = "size must be <= 100") int size,
            @RequestParam(required = false) String cursor) {
        SearchPage<SearchResult> data = searchService.search(query, type, size, cursor);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
