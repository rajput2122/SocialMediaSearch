package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.document.LocationDocument;
import com.practice.socialmediasearch.document.PageDocument;
import com.practice.socialmediasearch.document.PostDocument;
import com.practice.socialmediasearch.document.TagDocument;
import com.practice.socialmediasearch.document.UserDocument;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    static final String CIRCUIT_BREAKER_NAME = "elasticsearch";

    private final ElasticsearchOperations elasticsearchOperations;

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "searchFallback")
    public Page<SearchResult> search(String query, SearchType type, Pageable pageable) {
        if (query == null || query.isBlank() || type == null) {
            return Page.empty(pageable);
        }

        return switch (type) {
            case USER -> searchUsers(query, pageable);
            case POST -> searchPosts(query, pageable);
            case PAGE -> searchPages(query, pageable);
            case TAG -> searchTags(query, pageable);
            case LOCATION -> searchLocations(query, pageable);
        };
    }

    @SuppressWarnings("unused") // invoked reflectively by Resilience4j when ES fails or the breaker is open
    public Page<SearchResult> searchFallback(String query, SearchType type, Pageable pageable, Throwable ex) {
        log.warn("ES search fallback engaged for query='{}' type={}: {}", query, type, ex.toString());
        return Page.empty(pageable);
    }

    private Page<SearchResult> searchUsers(String query, Pageable pageable) {
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("name", "username", "bio")
                        .fuzziness("AUTO")))
                .withPageable(pageable)
                .build();

        SearchHits<UserDocument> hits = elasticsearchOperations.search(nativeQuery, UserDocument.class);
        List<SearchResult> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.USER)
                        .primaryText(doc.getName())
                        .secondaryText(doc.getUsername())
                        .locationName(doc.getLocationName())
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    private Page<SearchResult> searchPosts(String query, Pageable pageable) {
        // caption is text (fuzziness applies); tags is keyword (exact term match via should clause)
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .should(s -> s.multiMatch(mm -> mm
                                .query(query)
                                .fields("caption")
                                .fuzziness("AUTO")))
                        .should(s -> s.term(t -> t
                                .field("tags")
                                .value(query)))
                        .minimumShouldMatch("1")))
                .withPageable(pageable)
                .build();

        SearchHits<PostDocument> hits = elasticsearchOperations.search(nativeQuery, PostDocument.class);
        List<SearchResult> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.POST)
                        .primaryText(doc.getCaption())
                        .secondaryText(doc.getUserId())
                        .locationName(doc.getLocationName())
                        .tags(doc.getTags())
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    private Page<SearchResult> searchPages(String query, Pageable pageable) {
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("pageName", "bio")
                        .fuzziness("AUTO")))
                .withPageable(pageable)
                .build();

        SearchHits<PageDocument> hits = elasticsearchOperations.search(nativeQuery, PageDocument.class);
        List<SearchResult> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.PAGE)
                        .primaryText(doc.getPageName())
                        .secondaryText(doc.getBio())
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    private Page<SearchResult> searchTags(String query, Pageable pageable) {
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("tagName")
                        .fuzziness("AUTO")))
                .withPageable(pageable)
                .build();

        SearchHits<TagDocument> hits = elasticsearchOperations.search(nativeQuery, TagDocument.class);
        List<SearchResult> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.TAG)
                        .primaryText(doc.getTagName())
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }

    private Page<SearchResult> searchLocations(String query, Pageable pageable) {
        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("displayName")
                        .fuzziness("AUTO")))
                .withPageable(pageable)
                .build();

        SearchHits<LocationDocument> hits = elasticsearchOperations.search(nativeQuery, LocationDocument.class);
        List<SearchResult> content = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.LOCATION)
                        .primaryText(doc.getDisplayName())
                        .build())
                .toList();

        return new PageImpl<>(content, pageable, hits.getTotalHits());
    }
}

