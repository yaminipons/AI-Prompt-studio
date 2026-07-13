package com.promptstudio.enums;

/**
 * Represents the authorization role assigned to a user account.
 * Used by Spring Security to determine access to protected endpoints
 * (e.g., Admin Dashboard APIs are restricted to ADMIN role only).
 */
public enum UserRole {

    /** Standard user with access to all core prompt engineering features. */
    USER("Standard user with access to all core prompt engineering features"),

    /** Administrator with full platform access and user management capabilities. */
    ADMIN("Administrator with full platform access and user management capabilities");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the Spring Security authority string for this role,
     * prefixed with "ROLE_" as required by hasRole() convention.
     *
     * @return e.g. "ROLE_ADMIN"
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}