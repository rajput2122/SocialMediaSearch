package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.document.LocationDocument;
import com.practice.socialmediasearch.document.PageDocument;
import com.practice.socialmediasearch.document.PostDocument;
import com.practice.socialmediasearch.document.TagDocument;
import com.practice.socialmediasearch.document.UserDocument;
import com.practice.socialmediasearch.dto.CursorCodec;
import com.practice.socialmediasearch.dto.SearchPage;
import com.practice.socialmediasearch.dto.SearchResult;
import com.practice.socialmediasearch.dto.SearchType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SearchService {

    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("_score"), Sort.Order.asc("_id"));

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchPage<SearchResult> search(String query, SearchType type, int size, String cursor) {
        if (query == null || query.isBlank() || type == null) {
            return SearchPage.empty(size);
        }

        return switch (type) {
            case USER -> searchUsers(query, size, cursor);
            case POST -> searchPosts(query, size, cursor);
            case PAGE -> searchPages(query, size, cursor);
            case TAG -> searchTags(query, size, cursor);
            case LOCATION -> searchLocations(query, size, cursor);
        };
    }

    private SearchPage<SearchResult> searchUsers(String query, int size, String cursor) {
        return runSearch(size, cursor, UserDocument.class,
                qb -> qb.withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("name", "username", "bio")
                        .fuzziness("AUTO"))),
                doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.USER)
                        .primaryText(doc.getName())
                        .secondaryText(doc.getUsername())
                        .locationName(doc.getLocationName())
                        .build());
    }

    private SearchPage<SearchResult> searchPosts(String query, int size, String cursor) {
        return runSearch(size, cursor, PostDocument.class,
                qb -> qb.withQuery(q -> q.bool(b -> b
                        .should(s -> s.multiMatch(mm -> mm
                                .query(query)
                                .fields("caption")
                                .fuzziness("AUTO")))
                        .should(s -> s.term(t -> t
                                .field("tags")
                                .value(query)))
                        .minimumShouldMatch("1"))),
                doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.POST)
                        .primaryText(doc.getCaption())
                        .secondaryText(doc.getUserId())
                        .locationName(doc.getLocationName())
                        .tags(doc.getTags())
                        .build());
    }

    private SearchPage<SearchResult> searchPages(String query, int size, String cursor) {
        return runSearch(size, cursor, PageDocument.class,
                qb -> qb.withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("pageName", "bio")
                        .fuzziness("AUTO"))),
                doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.PAGE)
                        .primaryText(doc.getPageName())
                        .secondaryText(doc.getBio())
                        .build());
    }

    private SearchPage<SearchResult> searchTags(String query, int size, String cursor) {
        return runSearch(size, cursor, TagDocument.class,
                qb -> qb.withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("tagName")
                        .fuzziness("AUTO"))),
                doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.TAG)
                        .primaryText(doc.getTagName())
                        .build());
    }

    private SearchPage<SearchResult> searchLocations(String query, int size, String cursor) {
        return runSearch(size, cursor, LocationDocument.class,
                qb -> qb.withQuery(q -> q.multiMatch(mm -> mm
                        .query(query)
                        .fields("displayName")
                        .fuzziness("AUTO"))),
                doc -> SearchResult.builder()
                        .id(doc.getId())
                        .type(SearchType.LOCATION)
                        .primaryText(doc.getDisplayName())
                        .build());
    }

    private <T> SearchPage<SearchResult> runSearch(
            int size,
            String cursor,
            Class<T> docClass,
            Consumer<NativeQueryBuilder> queryCustomizer,
            Function<T, SearchResult> mapper) {

        NativeQueryBuilder builder = NativeQuery.builder()
                .withSort(DEFAULT_SORT)
                .withPageable(PageRequest.of(0, size));
        queryCustomizer.accept(builder);

        List<Object> searchAfter = CursorCodec.decode(cursor);
        if (searchAfter != null) {
            builder.withSearchAfter(searchAfter);
        }

        SearchHits<T> hits = elasticsearchOperations.search(builder.build(), docClass);
        List<SearchHit<T>> hitList = hits.getSearchHits();

        List<SearchResult> content = hitList.stream()
                .map(SearchHit::getContent)
                .map(mapper)
                .toList();

        String nextCursor = nextCursor(hitList, size);

        return SearchPage.<SearchResult>builder()
                .content(content)
                .size(size)
                .totalHits(hits.getTotalHits())
                .nextCursor(nextCursor)
                .build();
    }

    private <T> String nextCursor(List<SearchHit<T>> hits, int size) {
        if (hits.size() < size) {
            return null;
        }
        return CursorCodec.encode(hits.get(hits.size() - 1).getSortValues());
    }
}
