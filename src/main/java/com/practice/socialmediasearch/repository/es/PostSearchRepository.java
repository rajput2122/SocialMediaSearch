package com.practice.socialmediasearch.repository.es;

import com.practice.socialmediasearch.document.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PostSearchRepository extends ElasticsearchRepository<PostDocument, String> {
}

