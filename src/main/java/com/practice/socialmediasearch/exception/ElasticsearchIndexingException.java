package com.practice.socialmediasearch.exception;

public class ElasticsearchIndexingException extends RuntimeException {

    public ElasticsearchIndexingException(String entity, Object id, Throwable cause) {
        super("Failed to index " + entity + " with id: " + id, cause);
    }
}