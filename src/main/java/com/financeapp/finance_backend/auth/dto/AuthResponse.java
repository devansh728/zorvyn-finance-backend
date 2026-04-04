package com.financeapp.finance_backend.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType
) {
    public static AuthResponse of(String accessToken, String refreshToken, long expiresInMs) {
        return new AuthResponse(accessToken, refreshToken, expiresInMs / 1000, "Bearer");
    }
}
