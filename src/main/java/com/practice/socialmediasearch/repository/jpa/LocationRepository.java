package com.practice.socialmediasearch.repository.jpa;

import com.practice.socialmediasearch.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {
}

