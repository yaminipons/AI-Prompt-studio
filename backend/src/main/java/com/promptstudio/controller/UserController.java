package com.promptstudio.controller;

import com.promptstudio.dto.CommonDTOs.ApiResponse;
import com.promptstudio.dto.UserDTOs.ChangePasswordRequest;
import com.promptstudio.dto.UserDTOs.DashboardStatsResponse;
import com.promptstudio.dto.UserDTOs.UpdateProfileRequest;
import com.promptstudio.dto.UserDTOs.UserResponse;
import com.promptstudio.security.CustomUserDetailsService.UserPrincipal;
import com.promptstudio.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for the authenticated user's own
 * profile management and Dashboard statistics. Every endpoint here is
 * scoped to the caller via the injected {@link UserPrincipal}, so users
 * can never view or modify another account through this controller.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Authenticated user's profile and dashboard data")
public class UserController {

    private final UserService userService;

    /**
     * Retrieves the authenticated user's own profile.
     *
     * @param principal the authenticated user, injected by Spring Security
     * @return 200 OK with the UserResponse
     */
    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getProfile(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", response));
    }

    /**
     * Updates the authenticated user's editable profile fields.
     *
     * @param principal the authenticated user
     * @param request   the updated profile fields
     * @return 200 OK with the updated UserResponse
     */
    @PutMapping("/me")
    @Operation(summary = "Update the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse response = userService.updateProfile(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    /**
     * Changes the authenticated user's password.
     *
     * @param principal the authenticated user
     * @param request   the current and new password payload
     * @return 200 OK with no data payload
     */
    @PutMapping("/me/password")
    @Operation(summary = "Change the authenticated user's password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    /**
     * Retrieves aggregated statistics for the Dashboard home view.
     *
     * @param principal the authenticated user
     * @return 200 OK with the DashboardStatsResponse
     */
    @GetMapping("/me/dashboard-stats")
    @Operation(summary = "Get dashboard statistics for the authenticated user")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DashboardStatsResponse response = userService.getDashboardStats(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", response));
    }
}