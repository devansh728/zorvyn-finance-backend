package com.financeapp.finance_backend.auth;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.auth.dto.*;
import com.financeapp.finance_backend.auth.entity.RefreshToken;
import com.financeapp.finance_backend.auth.repository.RefreshTokenRepository;
import com.financeapp.finance_backend.auth.service.AuthServiceImpl;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.DuplicateResourceException;
import com.financeapp.finance_backend.security.blacklist.TokenBlacklistService;
import com.financeapp.finance_backend.security.jwt.JwtProperties;
import com.financeapp.finance_backend.security.jwt.JwtTokenProvider;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.repository.UserRepository;
import com.financeapp.finance_backend.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock JwtProperties jwtProperties;
    @Mock TokenBlacklistService tokenBlacklistService;
    @Mock AuditService auditService;
    @Mock UserServiceImpl userServiceImpl;

    @InjectMocks
    AuthServiceImpl authService;

    private User activeUser;
    private final String RAW_PASS = "Password1!";
    private final String HASHED_PASS = "$2a$12$hashedpassword";

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .email("user@test.com")
                .passwordHash(HASHED_PASS)
                .fullName("Test User")
                .role(UserRole.ANALYST)
                .status(UserStatus.ACTIVE)
                .build();
        // Reflectively set ID
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(activeUser, UUID.randomUUID());
        } catch (Exception ignored) {}
    }

    // ===== REGISTER =====

    @Test
    @DisplayName("register: success → user INACTIVE, VIEWER")
    void register_success_returnsInactiveUser() {
        var request = new RegisterRequest("new@test.com", RAW_PASS, "New User");

        when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(RAW_PASS)).thenReturn(HASHED_PASS);
        when(userRepository.save(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u;
        });
        when(userServiceImpl.toResponse(any())).thenCallRealMethod();

        var response = authService.register(request, "127.0.0.1");

        assertThat(response.status()).isEqualTo(UserStatus.INACTIVE);
        assertThat(response.role()).isEqualTo(UserRole.VIEWER);
        verify(auditService).log(eq("USER"), any(), eq("CREATE"), isNull(), isNull(), any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("register: duplicate email → DuplicateResourceException")
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("user@test.com", RAW_PASS, "Test"), "127.0.0.1"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ===== LOGIN =====

    @Test
    @DisplayName("login: correct credentials → returns tokens")
    void login_correctCredentials_returnsTokens() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(RAW_PASS, HASHED_PASS)).thenReturn(true);
        when(jwtTokenProvider.generateToken(any(), any(), any())).thenReturn("access.token");
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(86400000L);
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.login(
                new LoginRequest("user@test.com", RAW_PASS), "127.0.0.1", "test-agent");

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(0);
    }

    @Test
    @DisplayName("login: wrong password → BadCredentialsException, failedAttempts incremented")
    void login_wrongPassword_incrementsFailedAttempts() {
        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(activeUser);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("user@test.com", "wrongpass"), "127.0.0.1", null))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(activeUser.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("login: 5 failed attempts → account locked")
    void login_fiveFailedAttempts_locksAccount() {
        activeUser.setFailedLoginAttempts(4);

        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);
        when(userRepository.save(any())).thenReturn(activeUser);

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("user@test.com", "wrong"), "127.0.0.1", null))
                .isInstanceOf(BadCredentialsException.class);

        assertThat(activeUser.isAccountLocked()).isTrue();
        assertThat(activeUser.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("login: locked account → BusinessRuleException (no password check)")
    void login_lockedAccount_throws() {
        activeUser.setLockedUntil(Instant.now().plusSeconds(900));

        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("user@test.com", RAW_PASS), "127.0.0.1", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("login: inactive user → BusinessRuleException")
    void login_inactiveUser_throws() {
        activeUser.setStatus(UserStatus.INACTIVE);

        when(userRepository.findByEmailAndDeletedAtIsNull("user@test.com"))
                .thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("user@test.com", RAW_PASS), "127.0.0.1", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("login: unknown email → BadCredentialsException (no enumeration)")
    void login_unknownEmail_throwsGenericError() {
        when(userRepository.findByEmailAndDeletedAtIsNull("unknown@test.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                authService.login(new LoginRequest("unknown@test.com", RAW_PASS), "127.0.0.1", null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    // ===== REFRESH =====

    @Test
    @DisplayName("refresh: valid token → rotated, returns new access token")
    void refresh_validToken_returnsNewAccessToken() {
        String rawToken = "valid-refresh-token";
        String tokenHash = AuthServiceImpl.sha256(rawToken);

        RefreshToken storedToken = RefreshToken.builder()
                .user(activeUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateToken(any(), any(), any())).thenReturn("new.access.token");
        when(jwtProperties.getRefreshTokenExpiryMs()).thenReturn(86400000L);
        when(jwtProperties.getAccessTokenExpiryMs()).thenReturn(900000L);
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = authService.refresh(new RefreshTokenRequest(rawToken), "127.0.0.1", "agent");

        assertThat(response.accessToken()).isEqualTo("new.access.token");
        assertThat(storedToken.isRevoked()).isTrue(); // old token revoked
    }

    @Test
    @DisplayName("refresh: revoked token → BusinessRuleException")
    void refresh_revokedToken_throws() {
        String rawToken = "revoked-refresh-token";
        String tokenHash = AuthServiceImpl.sha256(rawToken);

        RefreshToken storedToken = RefreshToken.builder()
                .user(activeUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() ->
                authService.refresh(new RefreshTokenRequest(rawToken), "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("expired or been revoked");
    }

    @Test
    @DisplayName("refresh: expired token → BusinessRuleException")
    void refresh_expiredToken_throws() {
        String rawToken = "expired-token";
        String tokenHash = AuthServiceImpl.sha256(rawToken);

        RefreshToken expired = RefreshToken.builder()
                .user(activeUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(1)) // already expired
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() ->
                authService.refresh(new RefreshTokenRequest(rawToken), "127.0.0.1", "agent"))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ===== LOGOUT =====

    @Test
    @DisplayName("logout: valid tokens → access token blacklisted, refresh revoked")
    void logout_validTokens_revokesAll() {
        String rawRefresh = "refresh-token";
        String tokenHash = AuthServiceImpl.sha256(rawRefresh);

        RefreshToken storedToken = RefreshToken.builder()
                .user(activeUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(86400))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any())).thenReturn(storedToken);

        authService.logout("access.token", rawRefresh, "127.0.0.1");

        verify(tokenBlacklistService).blacklistToken("access.token");
        assertThat(storedToken.isRevoked()).isTrue();
    }
}
