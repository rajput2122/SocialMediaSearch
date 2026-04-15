package com.practice.socialmediasearch.repository.jpa;

import com.practice.socialmediasearch.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}

