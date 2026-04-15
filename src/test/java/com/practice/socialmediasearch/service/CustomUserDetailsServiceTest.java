package com.practice.socialmediasearch.service;

import com.practice.socialmediasearch.model.User;
import com.practice.socialmediasearch.model.UserCredential;
import com.practice.socialmediasearch.repository.jpa.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loadUserByUsernameReturnsUserDetails() {
        User user = User.builder().userId(1L).username("atulk").build();
        UserCredential credential = UserCredential.builder()
                .user(user)
                .password("$2a$10$hashedpassword")
                .build();

        when(userCredentialRepository.findByUser_Username("atulk")).thenReturn(Optional.of(credential));

        UserDetails details = customUserDetailsService.loadUserByUsername("atulk");

        assertThat(details.getUsername()).isEqualTo("atulk");
        assertThat(details.getPassword()).isEqualTo("$2a$10$hashedpassword");
        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_USER");
    }

    @Test
    void loadUserByUsernameThrowsWhenUserNotFound() {
        when(userCredentialRepository.findByUser_Username("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
