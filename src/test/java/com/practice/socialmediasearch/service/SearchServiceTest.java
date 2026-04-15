package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.document.*;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private SearchService searchService;

    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);
    }

    // ── null / blank / null type guards ──────────────────────────────────────

    @Test
    void searchReturnsEmptyPageWhenQueryIsNull() {
        Page<SearchResult> result = searchService.search(null, SearchType.USER, pageable);
        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void searchReturnsEmptyPageWhenQueryIsBlank() {
        Page<SearchResult> result = searchService.search("   ", SearchType.USER, pageable);
        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void searchReturnsEmptyPageWhenTypeIsNull() {
        Page<SearchResult> result = searchService.search("atul", null, pageable);
        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(elasticsearchOperations);
    }

    // ── USER ─────────────────────────────────────────────────────────────────

    @Test
    void searchUserMapsDocumentToResult() {
        UserDocument doc = UserDocument.builder()
                .id("1").name("Atul Kumar").username("atulk").bio("dev").locationName("Bengaluru")
                .build();

        SearchHits<UserDocument> hits = mockHits(List.of(doc), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        Page<SearchResult> page = searchService.search("atul", SearchType.USER, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("1");
        assertThat(r.getType()).isEqualTo(SearchType.USER);
        assertThat(r.getPrimaryText()).isEqualTo("Atul Kumar");
        assertThat(r.getSecondaryText()).isEqualTo("atulk");
        assertThat(r.getLocationName()).isEqualTo("Bengaluru");
    }

    @Test
    void searchUserReturnsEmptyPageWhenNoHits() {
        SearchHits<UserDocument> hits = mockHits(List.of(), 0L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        Page<SearchResult> page = searchService.search("unknown", SearchType.USER, pageable);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    void searchPostMapsDocumentToResult() {
        PostDocument doc = PostDocument.builder()
                .id("10").caption("Spring Boot tips").tags(List.of("spring")).locationName("Mumbai").userId("2")
                .build();

        SearchHits<PostDocument> hits = mockHits(List.of(doc), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PostDocument.class))).thenReturn(hits);

        Page<SearchResult> page = searchService.search("spring", SearchType.POST, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("10");
        assertThat(r.getType()).isEqualTo(SearchType.POST);
        assertThat(r.getPrimaryText()).isEqualTo("Spring Boot tips");
        assertThat(r.getTags()).containsExactly("spring");
        assertThat(r.getLocationName()).isEqualTo("Mumbai");
    }

    // ── PAGE ─────────────────────────────────────────────────────────────────

    @Test
    void searchPageMapsDocumentToResult() {
        PageDocument doc = PageDocument.builder()
                .id("20").pageName("Java Community").bio("For Java devs").locationName("Bengaluru")
                .build();

        SearchHits<PageDocument> hits = mockHits(List.of(doc), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PageDocument.class))).thenReturn(hits);

        Page<SearchResult> page = searchService.search("java", SearchType.PAGE, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("20");
        assertThat(r.getType()).isEqualTo(SearchType.PAGE);
        assertThat(r.getPrimaryText()).isEqualTo("Java Community");
        assertThat(r.getSecondaryText()).isEqualTo("For Java devs");
    }

    // ── TAG ──────────────────────────────────────────────────────────────────

    @Test
    void searchTagMapsDocumentToResult() {
        TagDocument doc = TagDocument.builder().id("30").tagName("microservices").build();

        SearchHits<TagDocument> hits = mockHits(List.of(doc), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(TagDocument.class))).thenReturn(hits);

        Page<SearchResult> page = searchService.search("microservices", SearchType.TAG, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("30");
        assertThat(r.getType()).isEqualTo(SearchType.TAG);
        assertThat(r.getPrimaryText()).isEqualTo("microservices");
    }

    // ── LOCATION ─────────────────────────────────────────────────────────────

    @Test
    void searchLocationMapsDocumentToResult() {
        LocationDocument doc = LocationDocument.builder().id("40").displayName("Bengaluru").build();

        SearchHits<LocationDocument> hits = mockHits(List.of(doc), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(LocationDocument.class))).thenReturn(hits);

        Page<SearchResult> page = searchService.search("bengaluru", SearchType.LOCATION, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("40");
        assertThat(r.getType()).isEqualTo(SearchType.LOCATION);
        assertThat(r.getPrimaryText()).isEqualTo("Bengaluru");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> SearchHits<T> mockHits(List<T> docs, long total) {
        SearchHits<T> hits = mock(SearchHits.class);
        List<SearchHit<T>> searchHits = docs.stream().map(doc -> {
            SearchHit<T> hit = mock(SearchHit.class);
            when(hit.getContent()).thenReturn(doc);
            return hit;
        }).toList();
        when(hits.getSearchHits()).thenReturn(searchHits);
        when(hits.getTotalHits()).thenReturn(total);
        return hits;
    }
}
