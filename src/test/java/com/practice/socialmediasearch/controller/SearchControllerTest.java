package com.practice.socialmediasearch.controller;

import com.practice.socialmediasearch.config.SecurityConfig;
import com.practice.socialmediasearch.dto.SearchPage;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import com.practice.socialmediasearch.exception.BadCursorException;
import com.practice.socialmediasearch.exception.GlobalExceptionHandler;
import com.practice.socialmediasearch.service.SearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    @WithMockUser
    void searchShouldReturnWrappedCursorPageResponse() throws Exception {
        SearchResult result = SearchResult.builder()
                .id("1")
                .type(SearchType.USER)
                .primaryText("Atul Kumar")
                .secondaryText("atulk")
                .locationName("Bengaluru")
                .build();

        SearchPage<SearchResult> page = SearchPage.<SearchResult>builder()
                .content(List.of(result))
                .size(10)
                .totalHits(1L)
                .nextCursor("encoded-cursor")
                .build();

        Mockito.when(searchService.search("atul", SearchType.USER, 10, null)).thenReturn(page);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("1"))
                .andExpect(jsonPath("$.data.content[0].type").value("USER"))
                .andExpect(jsonPath("$.data.content[0].primaryText").value("Atul Kumar"))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalHits").value(1))
                .andExpect(jsonPath("$.data.nextCursor").value("encoded-cursor"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    @WithMockUser
    void searchPassesCursorThroughToService() throws Exception {
        SearchPage<SearchResult> empty = SearchPage.empty(10);
        Mockito.when(searchService.search("atul", SearchType.USER, 10, "abc123")).thenReturn(empty);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .param("size", "10")
                        .param("cursor", "abc123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());

        Mockito.verify(searchService).search("atul", SearchType.USER, 10, "abc123");
    }

    @Test
    @WithMockUser
    void searchUsesDefaultSizeWhenSizeOmitted() throws Exception {
        Mockito.when(searchService.search("atul", SearchType.USER, 10, null))
                .thenReturn(SearchPage.empty(10));

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Mockito.verify(searchService).search("atul", SearchType.USER, 10, null);
    }

    @Test
    @WithMockUser
    void searchShouldReturnBadRequestForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", " ")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("query"));
    }

    @Test
    @WithMockUser
    void searchShouldReturnBadRequestForInvalidType() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "INVALID")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    @WithMockUser
    void searchShouldReturnBadRequestForSizeTooLarge() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .param("size", "500")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @WithMockUser
    void searchShouldReturnBadRequestForBadCursor() throws Exception {
        Mockito.when(searchService.search("atul", SearchType.USER, 10, "bad!!!"))
                .thenThrow(new BadCursorException("Invalid cursor"));

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .param("cursor", "bad!!!")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_CURSOR"));
    }

    @Test
    void searchShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
