package com.practice.socialmediasearch.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.practice.socialmediasearch.repository.jpa")
@EnableElasticsearchRepositories(basePackages = "com.practice.socialmediasearch.repository.es")
public class ElasticsearchConfig {
}
