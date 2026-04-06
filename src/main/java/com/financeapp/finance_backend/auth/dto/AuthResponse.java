package com.financeapp.finance_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication token response")
public record AuthResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9.access.payload")
        String accessToken,

    @Schema(description = "Refresh token used to obtain a new access token", example = "eyJhbGciOiJIUzI1NiJ9.refresh.payload")
        String refreshToken,

    @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn,

    @Schema(description = "Authorization scheme", example = "Bearer")
        String tokenType
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(accessToken, refreshToken, expiresInMs / 1000, "Bearer");
    }
}
