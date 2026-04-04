package com.financeapp.finance_backend.auth.controller;

import com.financeapp.finance_backend.auth.dto.*;
import com.financeapp.finance_backend.auth.service.AuthService;
import com.financeapp.finance_backend.common.dto.ApiResponse;
import com.financeapp.finance_backend.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Authentication", description = "Register, login, token refresh, logout")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user (pending admin approval)")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        UserResponse user = authService.register(request, httpRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "Registration successful. Your account is pending admin approval."));
    }

    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse authResponse = authService.login(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Login successful"));
    }

    @Operation(summary = "Refresh access token using a valid refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse authResponse = authService.refresh(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent"));

        return ResponseEntity.ok(ApiResponse.success(authResponse, "Token refreshed"));
    }

    @Operation(summary = "Logout and revoke tokens")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        String authHeader = httpRequest.getHeader("Authorization");
        String accessToken = null;
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }

        authService.logout(accessToken, request.refreshToken(), httpRequest.getRemoteAddr());
        return ResponseEntity.ok(ApiResponse.success(null, "Logged out successfully"));
    }
}
