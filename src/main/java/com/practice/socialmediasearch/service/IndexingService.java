package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.document.*;
import com.practice.socialmediasearch.exception.ElasticsearchIndexingException;
import com.practice.socialmediasearch.model.*;
import com.practice.socialmediasearch.repository.es.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingService {

    private final LocationSearchRepository locationSearchRepository;
    private final TagSearchRepository tagSearchRepository;
    private final UserSearchRepository userSearchRepository;
    private final PostSearchRepository postSearchRepository;
    private final PageSearchRepository pageSearchRepository;

    public void indexLocation(Location location) {
        try {
            LocationDocument document = LocationDocument.builder()
                    .id(String.valueOf(location.getLocationId()))
                    .displayName(location.getDisplayName())
                    .build();
            locationSearchRepository.save(document);
        } catch (Exception ex) {
            throw new ElasticsearchIndexingException("Location", location.getLocationId(), ex);
        }
    }

    public void indexTag(Tag tag) {
        try {
            TagDocument document = TagDocument.builder()
                    .id(String.valueOf(tag.getTagId()))
                    .tagName(tag.getTagName())
                    .build();
            tagSearchRepository.save(document);
        } catch (Exception ex) {
            throw new ElasticsearchIndexingException("Tag", tag.getTagId(), ex);
        }
    }

    public void indexUser(User user) {
        try {
            UserDocument document = UserDocument.builder()
                    .id(String.valueOf(user.getUserId()))
                    .name(user.getName())
                    .username(user.getUsername())
                    .bio(user.getBio())
                    .locationName(user.getLocation() != null ? user.getLocation().getDisplayName() : null)
                    .build();
            userSearchRepository.save(document);
        } catch (Exception ex) {
            throw new ElasticsearchIndexingException("User", user.getUserId(), ex);
        }
    }

    public void indexPost(Post post) {
        try {
            List<Tag> tags = post.getTags() != null ? post.getTags() : Collections.emptyList();

            PostDocument document = PostDocument.builder()
                    .id(String.valueOf(post.getPostId()))
                    .caption(post.getCaption())
                    .tags(tags.stream().map(Tag::getTagName).toList())
                    .locationName(post.getLocation() != null ? post.getLocation().getDisplayName() : null)
                    .userId(post.getUser() != null ? String.valueOf(post.getUser().getUserId()) : null)
                    .build();
            postSearchRepository.save(document);
        } catch (Exception ex) {
            throw new ElasticsearchIndexingException("Post", post.getPostId(), ex);
        }
    }

    public void indexPage(Page page) {
        try {
            PageDocument document = PageDocument.builder()
                    .id(String.valueOf(page.getPageId()))
                    .pageName(page.getPageName())
                    .bio(page.getBio())
                    .locationName(page.getLocation() != null ? page.getLocation().getDisplayName() : null)
                    .build();
            pageSearchRepository.save(document);
        } catch (Exception ex) {
            throw new ElasticsearchIndexingException("Page", page.getPageId(), ex);
        }
    }
}

