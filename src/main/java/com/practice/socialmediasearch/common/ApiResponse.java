package com.practice.socialmediasearch.common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiResponse<T> {

    private T data;
    private ErrorBody error;
    private MetaBody meta;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(MetaBody.builder().timestamp(LocalDateTime.now()).build())
                .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .error(ErrorBody.builder().code(code).message(message).build())
                .meta(MetaBody.builder().timestamp(LocalDateTime.now()).build())
                .build();
    }

    public static <T> ApiResponse<T> validationError(String message, List<FieldError> fieldErrors) {
        return ApiResponse.<T>builder()
                .error(ErrorBody.builder().code("VALIDATION_ERROR").message(message).fieldErrors(fieldErrors).build())
                .meta(MetaBody.builder().timestamp(LocalDateTime.now()).build())
                .build();
    }

    @Data
    @Builder
    public static class ErrorBody {
        private String code;
        private String message;
        private List<FieldError> fieldErrors;
    }

    @Data
    @Builder
    public static class MetaBody {
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}