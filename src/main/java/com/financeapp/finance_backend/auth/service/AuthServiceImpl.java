package com.financeapp.finance_backend.auth.service;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.auth.dto.*;
import com.financeapp.finance_backend.auth.entity.RefreshToken;
import com.financeapp.finance_backend.auth.repository.RefreshTokenRepository;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.DuplicateResourceException;
import com.financeapp.finance_backend.security.blacklist.TokenBlacklistService;
import com.financeapp.finance_backend.security.jwt.JwtProperties;
import com.financeapp.finance_backend.security.jwt.JwtTokenProvider;
import com.financeapp.finance_backend.user.dto.UserResponse;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.repository.UserRepository;
import com.financeapp.finance_backend.user.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditService auditService;
    private final UserServiceImpl userServiceImpl; // for toResponse mapper

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email().toLowerCase())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(UserRole.VIEWER)
                .status(UserStatus.INACTIVE) // Requires admin approval
                .build();

        user = userRepository.save(user);
        auditService.log("USER", user.getId(), "CREATE", null, null, userServiceImpl.toResponse(user), ipAddress);
        return userServiceImpl.toResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        // Use generic error to prevent email enumeration
        User user = userRepository.findByEmailAndDeletedAtIsNull(request.email().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (user.isAccountLocked()) {
            throw new BusinessRuleException("Account is temporarily locked. Please try again later.");
        }

        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessRuleException("Account is inactive. Please contact an administrator.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user, ipAddress);
            throw new BadCredentialsException("Invalid credentials");
        }

        // Reset failed attempts on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String fingerprint = sha256(userAgent != null ? userAgent : "");
        String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole().name(), fingerprint);
        String rawRefreshToken = generateSecureToken();
        String tokenHash = sha256(rawRefreshToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()))
                .build();
        refreshTokenRepository.save(refreshToken);

        auditService.log("USER", user.getId(), "LOGIN", user.getId(), null, null, ipAddress);
        return AuthResponse.of(accessToken, rawRefreshToken, jwtProperties.getAccessTokenExpiryMs());
    }

    @Override
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        String tokenHash = sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        if (stored.isRevoked() || stored.isExpired()) {
            throw new BusinessRuleException("Refresh token has expired or been revoked");
        }

        User user = stored.getUser();
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessRuleException("Account is inactive");
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        String fingerprint = sha256(userAgent != null ? userAgent : "");
        String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getRole().name(), fingerprint);
        String newRawToken = generateSecureToken();
        String newTokenHash = sha256(newRawToken);

        RefreshToken newRefreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(newTokenHash)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(Instant.now().plusMillis(jwtProperties.getRefreshTokenExpiryMs()))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        return AuthResponse.of(newAccessToken, newRawToken, jwtProperties.getAccessTokenExpiryMs());
    }

    @Override
    @Transactional
    public void logout(String accessToken, String refreshToken, String ipAddress) {
        // Blacklist access token immediately
        if (accessToken != null) {
            tokenBlacklistService.blacklistToken(accessToken);
        }

        // Revoke refresh token
        if (refreshToken != null) {
            String tokenHash = sha256(refreshToken);
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
                auditService.log("USER", rt.getUser().getId(), "LOGOUT", rt.getUser().getId(), null, null, ipAddress);
            });
        }
    }

    private void handleFailedLogin(User user, String ipAddress) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plusSeconds(LOCKOUT_DURATION_MINUTES * 60));
            log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), attempts);
        }

        userRepository.save(user);
        auditService.log("USER", user.getId(), "LOGIN_FAILED", null, null, null, ipAddress);
    }

    private String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
