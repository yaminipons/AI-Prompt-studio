package com.promptstudio.controller;

import com.promptstudio.dto.AuthDTOs.AuthResponse;
import com.promptstudio.dto.AuthDTOs.LoginRequest;
import com.promptstudio.dto.AuthDTOs.RegisterRequest;
import com.promptstudio.dto.CommonDTOs.ApiResponse;
import com.promptstudio.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints: registration and
 * login. Both endpoints are public (no JWT required) as configured in
 * SecurityConfig, and both issue a JWT immediately on success.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account and returns a JWT for immediate login.
     *
     * @param request the registration payload
     * @return 201 CREATED with the issued AuthResponse
     */
    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", response));
    }

    /**
     * Authenticates an existing user and returns a fresh JWT.
     *
     * @param request the login payload
     * @return 200 OK with the issued AuthResponse
     */
    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT access token")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }
}