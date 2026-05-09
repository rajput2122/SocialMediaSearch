package com.practice.socialmediasearch.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.practice.socialmediasearch.exception.BadCursorException;

import java.util.Base64;
import java.util.List;

public final class CursorCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CursorCodec() {}

    public static String encode(List<Object> sortValues) {
        if (sortValues == null || sortValues.isEmpty()) {
            return null;
        }
        try {
            byte[] json = MAPPER.writeValueAsBytes(sortValues);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode cursor", e);
        }
    }

    public static List<Object> decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            byte[] json = Base64.getUrlDecoder().decode(cursor);
            return MAPPER.readValue(json, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
            throw new BadCursorException("Invalid cursor");
        }
    }
}
