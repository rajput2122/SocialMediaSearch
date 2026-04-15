package com.practice.socialmediasearch.repository.es;

import com.practice.socialmediasearch.document.UserDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, String> {
}

