package com.promptstudio.controller;

import com.promptstudio.dto.CommonDTOs.ApiResponse;
import com.promptstudio.dto.CommonDTOs.PageResponse;
import com.promptstudio.dto.PromptDTOs.PromptResponse;
import com.promptstudio.dto.UserDTOs.UserResponse;
import com.promptstudio.service.AdminService;
import com.promptstudio.service.AdminService.AdminStatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller exposing Admin Dashboard functionality: user
 * management and platform-wide statistics/visibility. Every endpoint
 * under {@code /api/admin/**} is already restricted to users holding
 * ROLE_ADMIN by the security filter chain configured in
 * SecurityConfig, so no additional per-method authorization checks are
 * needed here.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Platform-wide user management and statistics (Admin only)")
public class AdminController {

    private final AdminService adminService;

    /**
     * Retrieves a paginated list of every registered user.
     *
     * @param page zero-based page number, defaults to 0
     * @param size page size, defaults to 20
     * @return 200 OK with a PageResponse of UserResponse
     */
    @GetMapping("/users")
    @Operation(summary = "List all registered users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<UserResponse> result = adminService.getAllUsers(page, size);
        PageResponse<UserResponse> response = new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success("Users retrieved", response));
    }

    /**
     * Retrieves a single user's full profile by ID.
     *
     * @param userId the ID of the user to retrieve
     * @return 200 OK with the UserResponse
     */
    @GetMapping("/users/{userId}")
    @Operation(summary = "Get a specific user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String userId) {
        UserResponse response = adminService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("User retrieved", response));
    }

    /**
     * Toggles a user's active status (activate/deactivate account).
     *
     * @param userId the ID of the user to toggle
     * @return 200 OK with the updated UserResponse
     */
    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Toggle a user's active status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable String userId) {
        UserResponse response = adminService.toggleUserActiveStatus(userId);
        String message = response.active() ? "User account activated" : "User account deactivated";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    /**
     * Updates a user's role (promote to ADMIN or demote to USER).
     *
     * @param userId the ID of the user whose role should be changed
     * @param body   a map containing the new "role" field ("USER" or "ADMIN")
     * @return 200 OK with the updated UserResponse
     */
    @PatchMapping("/users/{userId}/role")
    @Operation(summary = "Change a user's role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> body
    ) {
        UserResponse response = adminService.updateUserRole(userId, body.get("role"));
        return ResponseEntity.ok(ApiResponse.success("User role updated", response));
    }

    /**
     * Retrieves a paginated view of every prompt record across all users.
     *
     * @param page zero-based page number, defaults to 0
     * @param size page size, defaults to 20
     * @return 200 OK with a PageResponse of PromptResponse
     */
    @GetMapping("/prompts")
    @Operation(summary = "List all prompts across every user")
    public ResponseEntity<ApiResponse<PageResponse<PromptResponse>>> getAllPrompts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<PromptResponse> result = adminService.getAllPrompts(page, size);
        PageResponse<PromptResponse> response = new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success("Prompts retrieved", response));
    }

    /**
     * Retrieves aggregated platform-wide statistics for the Admin
     * Dashboard home view.
     *
     * @return 200 OK with the AdminStatsResponse
     */
    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide statistics")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getPlatformStats() {
        AdminStatsResponse response = adminService.getPlatformStats();
        return ResponseEntity.ok(ApiResponse.success("Platform stats retrieved", response));
    }
}