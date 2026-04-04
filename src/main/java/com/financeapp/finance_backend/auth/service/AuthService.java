package com.financeapp.finance_backend.auth.service;

import com.financeapp.finance_backend.auth.dto.*;
import com.financeapp.finance_backend.user.dto.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request, String ipAddress);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent);
    void logout(String accessToken, String refreshToken, String ipAddress);
}
