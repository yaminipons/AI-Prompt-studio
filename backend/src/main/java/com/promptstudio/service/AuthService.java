package com.promptstudio.service;

import com.promptstudio.dto.AuthDTOs.AuthResponse;
import com.promptstudio.dto.AuthDTOs.LoginRequest;
import com.promptstudio.dto.AuthDTOs.RegisterRequest;
import com.promptstudio.entity.User;
import com.promptstudio.enums.UserRole;
import com.promptstudio.exception.ApiException;
import com.promptstudio.repository.UserRepository;
import com.promptstudio.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service layer handling user registration and login. Delegates
 * credential verification to Spring Security's AuthenticationManager
 * and issues signed JWTs upon successful authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * Registers a new user account. Validates that the email is not
     * already in use, hashes the password with BCrypt, persists the
     * new user with the default USER role, and immediately issues a
     * JWT so the frontend can log the user in without a second request.
     *
     * @param request the registration payload
     * @return an AuthResponse containing the issued token and user info
     * @throws ApiException with 409 CONFLICT if the email is already registered
     */
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ApiException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        User newUser = User.builder()
                .fullName(request.fullName().trim())
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(UserRole.USER)
                .active(true)
                .lastLoginAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);

        String token = jwtUtil.generateToken(savedUser.getEmail(), savedUser.getId(), savedUser.getRole().name());

        return AuthResponse.of(
                token,
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    /**
     * Authenticates an existing user by email and password. Uses Spring
     * Security's AuthenticationManager (which internally invokes
     * CustomUserDetailsService and the configured PasswordEncoder) so
     * password comparison logic lives in exactly one place. On success,
     * updates the user's last login timestamp and issues a fresh JWT.
     *
     * @param request the login payload
     * @return an AuthResponse containing the issued token and user info
     * @throws ApiException with 401 UNAUTHORIZED if credentials are invalid or the account is disabled
     */
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
        } catch (BadCredentialsException ex) {
            throw new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED);
        } catch (DisabledException ex) {
            throw new ApiException("This account has been deactivated", HttpStatus.FORBIDDEN);
        }

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        return AuthResponse.of(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}