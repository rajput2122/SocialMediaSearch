package com.practice.socialmediasearch.exception;

import com.practice.socialmediasearch.config.SecurityConfig;
import com.practice.socialmediasearch.controller.SearchController;
import com.practice.socialmediasearch.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    @Test
    @WithMockUser
    void handlesMethodArgumentTypeMismatchWithBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "test")
                        .param("type", "INVALID_TYPE")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    @WithMockUser
    void handlesConstraintViolationForBlankQuery() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", " ")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.fieldErrors[0].field").value("query"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    @WithMockUser
    void handlesElasticsearchIndexingException() throws Exception {
        when(searchService.search(any(), any(), anyInt(), any()))
                .thenThrow(new ElasticsearchIndexingException("User", 1L, new RuntimeException("ES down")));

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "test")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INDEXING_ERROR"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    @WithMockUser
    void handlesGenericException() throws Exception {
        when(searchService.search(any(), any(), anyInt(), any()))
                .thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "test")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    @Test
    void returnsUnauthorizedWithJsonBodyForMissingCredentials() throws Exception {
        mockMvc.perform(get("/api/v1/search")
                        .param("q", "test")
                        .param("type", "USER")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.error.message").value("Authentication required"));
    }
}
