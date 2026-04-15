package com.practice.socialmediasearch.controller;

import com.practice.socialmediasearch.config.SecurityConfig;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import com.practice.socialmediasearch.exception.GlobalExceptionHandler;
import com.practice.socialmediasearch.service.SearchService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    void searchShouldReturnWrappedPageResponse() throws Exception {
        SearchResult result = SearchResult.builder()
                .id("1")
                .type(SearchType.USER)
                .primaryText("Atul Kumar")
                .secondaryText("atulk")
                .locationName("Bengaluru")
                .build();

        Mockito.when(searchService.search("atul", SearchType.USER, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(result), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("1"))
                .andExpect(jsonPath("$.data.content[0].type").value("USER"))
                .andExpect(jsonPath("$.data.content[0].primaryText").value("Atul Kumar"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.meta.timestamp").exists());
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
    void searchShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "atul")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}

