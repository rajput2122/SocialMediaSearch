package com.practice.socialmediasearch.model;

import com.practice.socialmediasearch.config.JpaConfig;
import com.practice.socialmediasearch.repository.jpa.LocationRepository;
import com.practice.socialmediasearch.repository.jpa.PageRepository;
import com.practice.socialmediasearch.repository.jpa.PostRepository;
import com.practice.socialmediasearch.repository.jpa.TagRepository;
import com.practice.socialmediasearch.repository.jpa.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaConfig.class)
class SoftDeleteAndAuditingTest {

    @Autowired TagRepository tagRepository;
    @Autowired LocationRepository locationRepository;
    @Autowired UserRepository userRepository;
    @Autowired PageRepository pageRepository;
    @Autowired PostRepository postRepository;
    @Autowired EntityManager entityManager;

    @Test
    void auditFieldsArePopulatedAutomaticallyOnSave() {
        Tag saved = tagRepository.saveAndFlush(Tag.builder().tagName("auditing-test").build());

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @Test
    void softDeletedTagIsHiddenFromFindByIdAndFindAll() {
        Tag tag = tagRepository.saveAndFlush(Tag.builder().tagName("soon-to-go").build());
        Long id = tag.getTagId();

        tag.setDeletedAt(LocalDateTime.now());
        tagRepository.saveAndFlush(tag);
        entityManager.clear();

        assertThat(tagRepository.findById(id)).isEmpty();
        assertThat(tagRepository.findAll()).extracting(Tag::getTagId).doesNotContain(id);
    }

    @Test
    void softDeletedLocationIsHiddenFromFindByIdAndFindAll() {
        Location loc = locationRepository.saveAndFlush(Location.builder().displayName("Atlantis").build());
        Long id = loc.getLocationId();

        loc.setDeletedAt(LocalDateTime.now());
        locationRepository.saveAndFlush(loc);
        entityManager.clear();

        assertThat(locationRepository.findById(id)).isEmpty();
        assertThat(locationRepository.findAll()).extracting(Location::getLocationId).doesNotContain(id);
    }

    @Test
    void softDeletedUserIsHiddenFromFindByIdAndFindAll() {
        User user = userRepository.saveAndFlush(User.builder()
                .name("Ghost").username("ghost").email("g@x.io").build());
        Long id = user.getUserId();

        user.setDeletedAt(LocalDateTime.now());
        userRepository.saveAndFlush(user);
        entityManager.clear();

        assertThat(userRepository.findById(id)).isEmpty();
        assertThat(userRepository.findAll()).extracting(User::getUserId).doesNotContain(id);
    }

    @Test
    void softDeletedPageIsHiddenFromFindByIdAndFindAll() {
        Page page = pageRepository.saveAndFlush(Page.builder().pageName("Old Page").build());
        Long id = page.getPageId();

        page.setDeletedAt(LocalDateTime.now());
        pageRepository.saveAndFlush(page);
        entityManager.clear();

        assertThat(pageRepository.findById(id)).isEmpty();
        assertThat(pageRepository.findAll()).extracting(Page::getPageId).doesNotContain(id);
    }

    @Test
    void softDeletedPostIsHiddenFromFindByIdAndFindAll() {
        User author = userRepository.saveAndFlush(User.builder()
                .name("Author").username("author").email("a@x.io").build());
        Post post = postRepository.saveAndFlush(Post.builder()
                .user(author).caption("doomed").postedOn(LocalDateTime.now()).build());
        Long id = post.getPostId();

        post.setDeletedAt(LocalDateTime.now());
        postRepository.saveAndFlush(post);
        entityManager.clear();

        assertThat(postRepository.findById(id)).isEmpty();
        assertThat(postRepository.findAll()).extracting(Post::getPostId).doesNotContain(id);
    }
}
