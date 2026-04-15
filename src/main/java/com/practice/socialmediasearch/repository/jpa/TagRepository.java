package com.practice.socialmediasearch.repository.jpa;

import com.practice.socialmediasearch.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, Long> {
}

