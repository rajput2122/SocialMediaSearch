package com.practice.socialmediasearch.repository.es;

import com.practice.socialmediasearch.document.PageDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PageSearchRepository extends ElasticsearchRepository<PageDocument, String> {
}

