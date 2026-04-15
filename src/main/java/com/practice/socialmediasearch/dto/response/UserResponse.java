package com.practice.socialmediasearch.dto.response;

import com.practice.socialmediasearch.model.Location;
import com.practice.socialmediasearch.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long userId;
    private String email;
    private String phone;
    private String name;
    private String username;
    private String bio;
    private Long locationId;
    private String locationName;

    public static UserResponse from(User user) {
        Location location = user.getLocation();

        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .name(user.getName())
                .username(user.getUsername())
                .bio(user.getBio())
                .locationId(location != null ? location.getLocationId() : null)
                .locationName(location != null ? location.getDisplayName() : null)
                .build();
    }
}

