package com.practice.socialmediasearch.repository.es;

import com.practice.socialmediasearch.document.TagDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface TagSearchRepository extends ElasticsearchRepository<TagDocument, String> {
}

