package com.promptstudio.security;

import com.promptstudio.entity.User;
import com.promptstudio.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Spring Security service that loads user-specific data during
 * authentication. Bridges our MongoDB {@link User} entity with
 * Spring Security's {@link UserDetails} contract via the nested
 * {@link UserPrincipal} wrapper, which also exposes the raw user ID
 * for convenient access in controllers (via @AuthenticationPrincipal).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email (used as the username) and wraps
     * them in a Spring Security-compatible UserDetails object.
     *
     * @param email the email to look up
     * @return the UserPrincipal wrapping the found user
     * @throws UsernameNotFoundException if no user with this email exists
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
        return new UserPrincipal(user);
    }

    /**
     * Spring Security-compatible wrapper around our {@link User} entity.
     * Implements {@link UserDetails} while exposing the underlying
     * MongoDB user ID directly, so controllers can retrieve it without
     * a second database lookup.
     */
    @Getter
    public static class UserPrincipal implements UserDetails {

        private final User user;

        public UserPrincipal(User user) {
            this.user = user;
        }

        /**
         * Returns the underlying MongoDB user ID.
         * Used throughout controllers to scope queries to the
         * authenticated user (e.g., findByUserId).
         */
        public String getUserId() {
            return user.getId();
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority(user.getRole().getAuthority()));
        }

        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return user.isActive();
        }
    }
}