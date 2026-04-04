package com.financeapp.finance_backend.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public class SecurityContextUtil {

    public static Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        
        try {
            // Assuming the principal is the user ID or we extract it from UserDetails
            // The actual implementation depends on how JwtAuthenticationFilter sets the authentication
            return Optional.of(UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
    
    public static UUID getCurrentUserIdOrThrow() {
        return getCurrentUserId().orElseThrow(() -> new IllegalStateException("User not authenticated or ID is invalid"));
    }
}
