package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.document.*;
import com.practice.socialmediasearch.exception.ElasticsearchIndexingException;
import com.practice.socialmediasearch.model.*;
import com.practice.socialmediasearch.repository.es.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexingServiceTest {

    @Mock LocationSearchRepository locationSearchRepository;
    @Mock TagSearchRepository tagSearchRepository;
    @Mock UserSearchRepository userSearchRepository;
    @Mock PostSearchRepository postSearchRepository;
    @Mock PageSearchRepository pageSearchRepository;
    @Mock ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private IndexingService indexingService;

    // ── indexLocation ─────────────────────────────────────────────────────────

    @Test
    void indexLocationSavesCorrectDocument() {
        Location location = Location.builder().locationId(1L).displayName("Bengaluru").build();

        indexingService.indexLocation(location);

        ArgumentCaptor<LocationDocument> captor = ArgumentCaptor.forClass(LocationDocument.class);
        verify(locationSearchRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("1");
        assertThat(captor.getValue().getDisplayName()).isEqualTo("Bengaluru");
    }

    @Test
    void indexLocationThrowsIndexingExceptionOnFailure() {
        Location location = Location.builder().locationId(1L).displayName("Bengaluru").build();
        when(locationSearchRepository.save(any())).thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.indexLocation(location))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Location")
                .hasMessageContaining("1");
    }

    // ── indexTag ──────────────────────────────────────────────────────────────

    @Test
    void indexTagSavesCorrectDocument() {
        Tag tag = Tag.builder().tagId(2L).tagName("spring").build();

        indexingService.indexTag(tag);

        ArgumentCaptor<TagDocument> captor = ArgumentCaptor.forClass(TagDocument.class);
        verify(tagSearchRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("2");
        assertThat(captor.getValue().getTagName()).isEqualTo("spring");
    }

    @Test
    void indexTagThrowsIndexingExceptionOnFailure() {
        Tag tag = Tag.builder().tagId(2L).tagName("spring").build();
        when(tagSearchRepository.save(any())).thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.indexTag(tag))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Tag");
    }

    // ── indexUser ─────────────────────────────────────────────────────────────

    @Test
    void indexUserSavesCorrectDocument() {
        Location loc = Location.builder().locationId(1L).displayName("Bengaluru").build();
        User user = User.builder()
                .userId(5L).name("Atul Kumar").username("atulk").bio("dev").location(loc)
                .build();

        indexingService.indexUser(user);

        ArgumentCaptor<UserDocument> captor = ArgumentCaptor.forClass(UserDocument.class);
        verify(userSearchRepository).save(captor.capture());
        UserDocument doc = captor.getValue();
        assertThat(doc.getId()).isEqualTo("5");
        assertThat(doc.getName()).isEqualTo("Atul Kumar");
        assertThat(doc.getUsername()).isEqualTo("atulk");
        assertThat(doc.getBio()).isEqualTo("dev");
        assertThat(doc.getLocationName()).isEqualTo("Bengaluru");
    }

    @Test
    void indexUserHandlesNullLocation() {
        User user = User.builder().userId(5L).name("Atul").username("atulk").build();

        indexingService.indexUser(user);

        ArgumentCaptor<UserDocument> captor = ArgumentCaptor.forClass(UserDocument.class);
        verify(userSearchRepository).save(captor.capture());
        assertThat(captor.getValue().getLocationName()).isNull();
    }

    @Test
    void indexUserThrowsIndexingExceptionOnFailure() {
        User user = User.builder().userId(5L).name("Atul").username("atulk").build();
        when(userSearchRepository.save(any())).thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.indexUser(user))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("User");
    }

    // ── indexPost ─────────────────────────────────────────────────────────────

    @Test
    void indexPostSavesCorrectDocument() {
        Location loc = Location.builder().locationId(1L).displayName("Mumbai").build();
        User user = User.builder().userId(3L).build();
        Tag t1 = Tag.builder().tagId(1L).tagName("java").build();
        Tag t2 = Tag.builder().tagId(2L).tagName("spring").build();
        Post post = Post.builder()
                .postId(10L).caption("Learning Spring").location(loc).user(user).tags(List.of(t1, t2))
                .build();

        indexingService.indexPost(post);

        ArgumentCaptor<PostDocument> captor = ArgumentCaptor.forClass(PostDocument.class);
        verify(postSearchRepository).save(captor.capture());
        PostDocument doc = captor.getValue();
        assertThat(doc.getId()).isEqualTo("10");
        assertThat(doc.getCaption()).isEqualTo("Learning Spring");
        assertThat(doc.getTags()).containsExactly("java", "spring");
        assertThat(doc.getLocationName()).isEqualTo("Mumbai");
        assertThat(doc.getUserId()).isEqualTo("3");
    }

    @Test
    void indexPostHandlesNullTagsAndNullLocationAndNullUser() {
        Post post = Post.builder().postId(10L).caption("No meta").build();

        indexingService.indexPost(post);

        ArgumentCaptor<PostDocument> captor = ArgumentCaptor.forClass(PostDocument.class);
        verify(postSearchRepository).save(captor.capture());
        PostDocument doc = captor.getValue();
        assertThat(doc.getTags()).isEmpty();
        assertThat(doc.getLocationName()).isNull();
        assertThat(doc.getUserId()).isNull();
    }

    @Test
    void indexPostThrowsIndexingExceptionOnFailure() {
        Post post = Post.builder().postId(10L).caption("x").build();
        when(postSearchRepository.save(any())).thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.indexPost(post))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Post");
    }

    // ── indexPage ─────────────────────────────────────────────────────────────

    @Test
    void indexPageSavesCorrectDocument() {
        Location loc = Location.builder().locationId(1L).displayName("Bengaluru").build();
        com.practice.socialmediasearch.model.Page page =
                com.practice.socialmediasearch.model.Page.builder()
                        .pageId(20L).pageName("Java Community").bio("For devs").location(loc)
                        .build();

        indexingService.indexPage(page);

        ArgumentCaptor<PageDocument> captor = ArgumentCaptor.forClass(PageDocument.class);
        verify(pageSearchRepository).save(captor.capture());
        PageDocument doc = captor.getValue();
        assertThat(doc.getId()).isEqualTo("20");
        assertThat(doc.getPageName()).isEqualTo("Java Community");
        assertThat(doc.getBio()).isEqualTo("For devs");
        assertThat(doc.getLocationName()).isEqualTo("Bengaluru");
    }

    @Test
    void indexPageHandlesNullLocation() {
        com.practice.socialmediasearch.model.Page page =
                com.practice.socialmediasearch.model.Page.builder()
                        .pageId(20L).pageName("No Location Page").build();

        indexingService.indexPage(page);

        ArgumentCaptor<PageDocument> captor = ArgumentCaptor.forClass(PageDocument.class);
        verify(pageSearchRepository).save(captor.capture());
        assertThat(captor.getValue().getLocationName()).isNull();
    }

    @Test
    void indexPageThrowsIndexingExceptionOnFailure() {
        com.practice.socialmediasearch.model.Page page =
                com.practice.socialmediasearch.model.Page.builder().pageId(20L).pageName("x").build();
        when(pageSearchRepository.save(any())).thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.indexPage(page))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Page");
    }

    // ── delete*Document ───────────────────────────────────────────────────────

    @Test
    void deleteTagDocumentRemovesByIdFromTagIndex() {
        indexingService.deleteTagDocument(2L);
        verify(elasticsearchOperations).delete("2", TagDocument.class);
    }

    @Test
    void deleteTagDocumentThrowsIndexingExceptionOnFailure() {
        when(elasticsearchOperations.delete(eq("2"), eq(TagDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.deleteTagDocument(2L))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Tag")
                .hasMessageContaining("2");
    }

    @Test
    void deleteLocationDocumentRemovesByIdFromLocationIndex() {
        indexingService.deleteLocationDocument(1L);
        verify(elasticsearchOperations).delete("1", LocationDocument.class);
    }

    @Test
    void deleteLocationDocumentThrowsIndexingExceptionOnFailure() {
        when(elasticsearchOperations.delete(eq("1"), eq(LocationDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.deleteLocationDocument(1L))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Location");
    }

    @Test
    void deleteUserDocumentRemovesByIdFromUserIndex() {
        indexingService.deleteUserDocument(5L);
        verify(elasticsearchOperations).delete("5", UserDocument.class);
    }

    @Test
    void deleteUserDocumentThrowsIndexingExceptionOnFailure() {
        when(elasticsearchOperations.delete(eq("5"), eq(UserDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.deleteUserDocument(5L))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("User");
    }

    @Test
    void deletePageDocumentRemovesByIdFromPageIndex() {
        indexingService.deletePageDocument(20L);
        verify(elasticsearchOperations).delete("20", PageDocument.class);
    }

    @Test
    void deletePageDocumentThrowsIndexingExceptionOnFailure() {
        when(elasticsearchOperations.delete(eq("20"), eq(PageDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.deletePageDocument(20L))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Page");
    }

    @Test
    void deletePostDocumentRemovesByIdFromPostIndex() {
        indexingService.deletePostDocument(10L);
        verify(elasticsearchOperations).delete("10", PostDocument.class);
    }

    @Test
    void deletePostDocumentThrowsIndexingExceptionOnFailure() {
        when(elasticsearchOperations.delete(eq("10"), eq(PostDocument.class)))
                .thenThrow(new RuntimeException("ES down"));

        assertThatThrownBy(() -> indexingService.deletePostDocument(10L))
                .isInstanceOf(ElasticsearchIndexingException.class)
                .hasMessageContaining("Post");
    }
}
