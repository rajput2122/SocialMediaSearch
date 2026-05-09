package com.practice.socialmediasearch.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Duration;

@Configuration
@EnableJpaRepositories(basePackages = "com.practice.socialmediasearch.repository.jpa")
@EnableElasticsearchRepositories(basePackages = "com.practice.socialmediasearch.repository.es")
public class ElasticsearchConfig {

    @Bean
    public RestClientBuilderCustomizer elasticsearchTimeoutsCustomizer(
            @Value("${spring.elasticsearch.connection-timeout:2s}") Duration connectTimeout,
            @Value("${spring.elasticsearch.socket-timeout:5s}") Duration socketTimeout) {
        // Explicit request-config callback. Without finite timeouts a slow ES will
        // pin every Tomcat worker indefinitely and cascade into a full app outage.
        return builder -> builder.setRequestConfigCallback(config -> config
                .setConnectTimeout((int) connectTimeout.toMillis())
                .setSocketTimeout((int) socketTimeout.toMillis()));
    }
}
