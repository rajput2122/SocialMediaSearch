package com.practice.socialmediasearch.config;

import com.practice.socialmediasearch.model.*;
import com.practice.socialmediasearch.repository.jpa.*;
import com.practice.socialmediasearch.service.IndexingEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final LocationRepository locationRepository;
    private final TagRepository tagRepository;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final PostRepository postRepository;
    private final PageRepository pageRepository;
    private final IndexingEventPublisher indexingEventPublisher;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("Seeding sample data into H2 and Elasticsearch...");

        // Locations
        Location bengaluru = locationRepository.save(Location.builder().displayName("Bengaluru").build());
        Location mumbai = locationRepository.save(Location.builder().displayName("Mumbai").build());
        indexingEventPublisher.publishLocation(bengaluru);
        indexingEventPublisher.publishLocation(mumbai);

        // Tags
        Tag javaTag = tagRepository.save(Tag.builder().tagName("java").build());
        Tag springTag = tagRepository.save(Tag.builder().tagName("spring").build());
        Tag microservicesTag = tagRepository.save(Tag.builder().tagName("microservices").build());
        indexingEventPublisher.publishTag(javaTag);
        indexingEventPublisher.publishTag(springTag);
        indexingEventPublisher.publishTag(microservicesTag);

        // Users
        User atulk = userRepository.save(User.builder()
                .name("Atul Kumar")
                .username("atulk")
                .email("atul@example.com")
                .phone("9000000001")
                .bio("Java developer passionate about microservices")
                .location(bengaluru)
                .build());
        userCredentialRepository.save(UserCredential.builder()
                .user(atulk)
                .password(passwordEncoder.encode("password123"))
                .build());
        indexingEventPublisher.publishUser(atulk);

        User priyas = userRepository.save(User.builder()
                .name("Priya Sharma")
                .username("priyas")
                .email("priya@example.com")
                .phone("9000000002")
                .bio("Spring enthusiast and cloud native advocate")
                .location(mumbai)
                .build());
        userCredentialRepository.save(UserCredential.builder()
                .user(priyas)
                .password(passwordEncoder.encode("password123"))
                .build());
        indexingEventPublisher.publishUser(priyas);

        // Posts
        Post post1 = postRepository.save(Post.builder()
                .caption("Learning Spring Boot with microservices architecture")
                .user(atulk)
                .location(bengaluru)
                .postedOn(LocalDateTime.now())
                .tags(List.of(javaTag, springTag))
                .build());
        indexingEventPublisher.publishPost(post1);

        Post post2 = postRepository.save(Post.builder()
                .caption("Exploring cloud native applications with Spring")
                .user(priyas)
                .location(mumbai)
                .postedOn(LocalDateTime.now())
                .tags(List.of(springTag, microservicesTag))
                .build());
        indexingEventPublisher.publishPost(post2);

        // Pages
        Page javaPage = pageRepository.save(Page.builder()
                .pageName("Java Community")
                .bio("A community for Java developers to connect and share")
                .location(bengaluru)
                .build());
        indexingEventPublisher.publishPage(javaPage);

        Page springPage = pageRepository.save(Page.builder()
                .pageName("Spring Framework Fans")
                .bio("Discussions around the Spring ecosystem")
                .location(mumbai)
                .build());
        indexingEventPublisher.publishPage(springPage);

        log.info("Seed data loaded. Test credentials — username: atulk, password: password123");
    }
}
