package com.practice.socialmediasearch.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void successWrapsDataAndSetsTimestamp() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getError()).isNull();
        assertThat(response.getMeta()).isNotNull();
        assertThat(response.getMeta().getTimestamp()).isNotNull();
    }

    @Test
    void errorSetsCodeAndMessageAndNullData() {
        ApiResponse<Void> response = ApiResponse.error("NOT_FOUND", "Resource missing");

        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("NOT_FOUND");
        assertThat(response.getError().getMessage()).isEqualTo("Resource missing");
        assertThat(response.getError().getFieldErrors()).isNull();
        assertThat(response.getMeta().getTimestamp()).isNotNull();
    }

    @Test
    void validationErrorSetsCodeAndFieldErrors() {
        List<ApiResponse.FieldError> fieldErrors = List.of(
                ApiResponse.FieldError.builder().field("q").message("q is required").build()
        );

        ApiResponse<Void> response = ApiResponse.validationError("Validation failed", fieldErrors);

        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getError().getMessage()).isEqualTo("Validation failed");
        assertThat(response.getError().getFieldErrors()).hasSize(1);
        assertThat(response.getError().getFieldErrors().get(0).getField()).isEqualTo("q");
        assertThat(response.getError().getFieldErrors().get(0).getMessage()).isEqualTo("q is required");
    }
}
