package com.practice.socialmediasearch.repository.es;

import com.practice.socialmediasearch.document.LocationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface LocationSearchRepository extends ElasticsearchRepository<LocationDocument, String> {
}

