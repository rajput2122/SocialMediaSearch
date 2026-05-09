package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.document.*;
import com.practice.socialmediasearch.dto.CursorCodec;
import com.practice.socialmediasearch.dto.SearchPage;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import com.practice.socialmediasearch.exception.BadCursorException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private SearchService searchService;

    private static final int PAGE_SIZE = 10;

    // ── null / blank / null type guards ──────────────────────────────────────

    @Test
    void searchReturnsEmptyPageWhenQueryIsNull() {
        SearchPage<SearchResult> result = searchService.search(null, SearchType.USER, PAGE_SIZE, null);
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getNextCursor()).isNull();
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void searchReturnsEmptyPageWhenQueryIsBlank() {
        SearchPage<SearchResult> result = searchService.search("   ", SearchType.USER, PAGE_SIZE, null);
        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    void searchReturnsEmptyPageWhenTypeIsNull() {
        SearchPage<SearchResult> result = searchService.search("atul", null, PAGE_SIZE, null);
        assertThat(result.getContent()).isEmpty();
        verifyNoInteractions(elasticsearchOperations);
    }

    // ── USER ─────────────────────────────────────────────────────────────────

    @Test
    void searchUserMapsDocumentToResult() {
        UserDocument doc = UserDocument.builder()
                .id("1").name("Atul Kumar").username("atulk").bio("dev").locationName("Bengaluru")
                .build();

        SearchHits<UserDocument> hits = mockHits(List.of(hit(doc, List.of(1.5, "1"))), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("atul", SearchType.USER, PAGE_SIZE, null);

        assertThat(page.getTotalHits()).isEqualTo(1);
        assertThat(page.getNextCursor()).isNull(); // hits.size() < page size → no more pages
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

        SearchPage<SearchResult> page = searchService.search("unknown", SearchType.USER, PAGE_SIZE, null);

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalHits()).isEqualTo(0);
        assertThat(page.getNextCursor()).isNull();
    }

    // ── POST ─────────────────────────────────────────────────────────────────

    @Test
    void searchPostMapsDocumentToResult() {
        PostDocument doc = PostDocument.builder()
                .id("10").caption("Spring Boot tips").tags(List.of("spring")).locationName("Mumbai").userId("2")
                .build();

        SearchHits<PostDocument> hits = mockHits(List.of(hit(doc, List.of(2.0, "10"))), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PostDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("spring", SearchType.POST, PAGE_SIZE, null);

        assertThat(page.getTotalHits()).isEqualTo(1);
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

        SearchHits<PageDocument> hits = mockHits(List.of(hit(doc, List.of(1.0, "20"))), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(PageDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("java", SearchType.PAGE, PAGE_SIZE, null);

        assertThat(page.getTotalHits()).isEqualTo(1);
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

        SearchHits<TagDocument> hits = mockHits(List.of(hit(doc, List.of(1.0, "30"))), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(TagDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("microservices", SearchType.TAG, PAGE_SIZE, null);

        assertThat(page.getTotalHits()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("30");
        assertThat(r.getType()).isEqualTo(SearchType.TAG);
        assertThat(r.getPrimaryText()).isEqualTo("microservices");
    }

    // ── LOCATION ─────────────────────────────────────────────────────────────

    @Test
    void searchLocationMapsDocumentToResult() {
        LocationDocument doc = LocationDocument.builder().id("40").displayName("Bengaluru").build();

        SearchHits<LocationDocument> hits = mockHits(List.of(hit(doc, List.of(1.0, "40"))), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(LocationDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("bengaluru", SearchType.LOCATION, PAGE_SIZE, null);

        assertThat(page.getTotalHits()).isEqualTo(1);
        SearchResult r = page.getContent().get(0);
        assertThat(r.getId()).isEqualTo("40");
        assertThat(r.getType()).isEqualTo(SearchType.LOCATION);
        assertThat(r.getPrimaryText()).isEqualTo("Bengaluru");
    }

    // ── search_after pagination ──────────────────────────────────────────────

    @Test
    void firstPageWithoutCursorOmitsSearchAfter() {
        UserDocument doc = UserDocument.builder().id("1").name("Atul").username("atulk").build();
        SearchHits<UserDocument> hits = mockHits(List.of(hit(doc, List.of(1.5, "1"))), 1L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        searchService.search("atul", SearchType.USER, PAGE_SIZE, null);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(UserDocument.class));
        NativeQuery sent = queryCaptor.getValue();

        assertThat(sent.getSearchAfter()).isNull();
        assertThat(sent.getSort()).isNotNull();
        assertThat(sent.getPageable().getPageNumber()).isEqualTo(0);
        assertThat(sent.getPageable().getPageSize()).isEqualTo(PAGE_SIZE);
    }

    @Test
    void subsequentPageWithCursorPassesSearchAfter() {
        String cursor = CursorCodec.encode(List.of(1.5, "1"));

        UserDocument doc = UserDocument.builder().id("2").name("Priya").username("priyas").build();
        SearchHits<UserDocument> hits = mockHits(List.of(hit(doc, List.of(1.0, "2"))), 99L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        searchService.search("a", SearchType.USER, PAGE_SIZE, cursor);

        ArgumentCaptor<NativeQuery> queryCaptor = ArgumentCaptor.forClass(NativeQuery.class);
        verify(elasticsearchOperations).search(queryCaptor.capture(), eq(UserDocument.class));
        NativeQuery sent = queryCaptor.getValue();

        assertThat(sent.getSearchAfter()).containsExactly(1.5, "1");
        // from must remain 0 even with a cursor — search_after replaces from-based pagination
        assertThat(sent.getPageable().getPageNumber()).isEqualTo(0);
    }

    @Test
    void nextCursorIsEncodedFromLastHitWhenPageIsFull() {
        List<SearchHit<UserDocument>> hitList = List.of(
                hit(UserDocument.builder().id("1").name("a").username("a").build(), List.of(2.0, "1")),
                hit(UserDocument.builder().id("2").name("b").username("b").build(), List.of(1.0, "2"))
        );
        SearchHits<UserDocument> hits = mockHits(hitList, 5L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("x", SearchType.USER, 2, null);

        assertThat(page.getNextCursor()).isNotNull();
        assertThat(CursorCodec.decode(page.getNextCursor())).containsExactly(1.0, "2");
    }

    @Test
    void nextCursorIsNullWhenLastPage() {
        // requested size 5 but only 2 hits returned → no more pages
        List<SearchHit<UserDocument>> hitList = List.of(
                hit(UserDocument.builder().id("1").name("a").username("a").build(), List.of(2.0, "1")),
                hit(UserDocument.builder().id("2").name("b").username("b").build(), List.of(1.0, "2"))
        );
        SearchHits<UserDocument> hits = mockHits(hitList, 2L);
        when(elasticsearchOperations.search(any(NativeQuery.class), eq(UserDocument.class))).thenReturn(hits);

        SearchPage<SearchResult> page = searchService.search("x", SearchType.USER, 5, null);

        assertThat(page.getNextCursor()).isNull();
    }

    @Test
    void invalidCursorThrowsBadCursorException() {
        assertThatThrownBy(() -> searchService.search("atul", SearchType.USER, PAGE_SIZE, "not-base64!!!"))
                .isInstanceOf(BadCursorException.class);
        verifyNoInteractions(elasticsearchOperations);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private <T> SearchHit<T> hit(T doc, List<Object> sortValues) {
        SearchHit<T> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        lenient().when(hit.getSortValues()).thenReturn(sortValues);
        return hit;
    }

    @SuppressWarnings("unchecked")
    private <T> SearchHits<T> mockHits(List<SearchHit<T>> hits, long total) {
        SearchHits<T> result = mock(SearchHits.class);
        when(result.getSearchHits()).thenReturn(hits);
        when(result.getTotalHits()).thenReturn(total);
        return result;
    }
}
