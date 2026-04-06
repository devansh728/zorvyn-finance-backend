package com.financeapp.finance_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request payload for refreshing an access token")
public record RefreshTokenRequest(
	@Schema(description = "Valid refresh token issued at login", example = "eyJhbGciOiJIUzI1NiJ9.refresh.payload")
	@NotBlank String refreshToken) {}
