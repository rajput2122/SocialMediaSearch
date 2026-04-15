package com.practice.socialmediasearch.controller;

import com.practice.socialmediasearch.common.ApiResponse;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import com.practice.socialmediasearch.service.SearchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
    public ResponseEntity<ApiResponse<Page<SearchResult>>> search(
            @RequestParam("q") @NotBlank(message = "q is required") String query,
            @RequestParam SearchType type,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<SearchResult> data = searchService.search(query, type, pageable);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}

