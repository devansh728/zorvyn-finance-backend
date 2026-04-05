package com.financeapp.finance_backend.user;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.DuplicateResourceException;
import com.financeapp.finance_backend.common.exception.ResourceNotFoundException;
import com.financeapp.finance_backend.user.dto.*;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;

    @InjectMocks
    UserServiceImpl userService;

    private UUID adminId;
    private UUID targetUserId;
    private User adminUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        adminId = UUID.randomUUID();
        targetUserId = UUID.randomUUID();

        adminUser = buildUser(adminId, "admin@test.com", UserRole.ADMIN, UserStatus.ACTIVE);
        targetUser = buildUser(targetUserId, "user@test.com", UserRole.ANALYST, UserStatus.ACTIVE);
    }

    private User buildUser(UUID id, String email, UserRole role, UserStatus status) {
        User u = User.builder()
                .email(email)
                .passwordHash("hashed")
                .fullName("Test " + role)
                .role(role)
                .status(status)
                .build();
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(u, id);
        } catch (Exception ignored) {}
        return u;
    }

    // ===== GET ME =====

    @Test
    @DisplayName("getMe: returns current user data")
    void getMe_returnsCurrentUser() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        UserResponse response = userService.getMe(adminId);

        assertThat(response.email()).isEqualTo("admin@test.com");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    @DisplayName("getMe: deleted user → ResourceNotFoundException")
    void getMe_deletedUser_throws() {
        adminUser.setDeletedAt(java.time.Instant.now());
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.getMe(adminId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== CREATE USER =====

    @Test
    @DisplayName("createUser: unique email → creates and returns")
    void createUser_uniqueEmail_success() {
        var request = new CreateUserRequest("new@test.com", "Password1!", "New User", UserRole.ANALYST);

        when(userRepository.existsByEmailAndDeletedAtIsNull("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1!")).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.createUser(request, adminId, "127.0.0.1");

        assertThat(response.email()).isEqualTo("new@test.com");
        assertThat(response.role()).isEqualTo(UserRole.ANALYST);
        verify(auditService).log(eq("USER"), any(), eq("CREATE"), eq(adminId), isNull(), any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("createUser: duplicate email → DuplicateResourceException")
    void createUser_duplicateEmail_throws() {
        when(userRepository.existsByEmailAndDeletedAtIsNull("admin@test.com")).thenReturn(true);

        var request = new CreateUserRequest("admin@test.com", "Password1!", "Dup", UserRole.VIEWER);
        assertThatThrownBy(() -> userService.createUser(request, adminId, "127.0.0.1"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    // ===== UPDATE STATUS =====

    @Test
    @DisplayName("updateStatus: deactivate analyst → succeeds")
    void updateStatus_deactivateAnalyst_success() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.updateStatus(
                targetUserId, new UpdateStatusRequest(UserStatus.INACTIVE), adminId, "127.0.0.1");

        assertThat(result.status()).isEqualTo(UserStatus.INACTIVE);
        verify(auditService).log(eq("USER"), eq(targetUserId), eq("STATUS_CHANGE"), eq(adminId), any(), any(), anyString());
    }

    @Test
    @DisplayName("updateStatus: deactivate last admin → BusinessRuleException")
    void updateStatus_lastAdminDeactivate_throws() {
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateStatus(
                adminId, new UpdateStatusRequest(UserStatus.INACTIVE), adminId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last active admin");
    }

    // ===== UPDATE ROLE =====

    @Test
    @DisplayName("updateRole: change analyst to viewer → succeeds")
    void updateRole_analystToViewer_success() {
        when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID differentAdmin = UUID.randomUUID();
        UserResponse result = userService.updateRole(
                targetUserId, new UpdateRoleRequest(UserRole.VIEWER), differentAdmin, "127.0.0.1");

        assertThat(result.role()).isEqualTo(UserRole.VIEWER);
    }

    @Test
    @DisplayName("updateRole: self-demotion → BusinessRuleException")
    void updateRole_selfDemotion_throws() {
        assertThatThrownBy(() -> userService.updateRole(
                adminId, new UpdateRoleRequest(UserRole.VIEWER), adminId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("own role");
    }

    @Test
    @DisplayName("updateRole: demote last admin → BusinessRuleException")
    void updateRole_lastAdmin_throws() {
        UUID otherId = UUID.randomUUID();
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.countActiveAdmins()).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRole(
                adminId, new UpdateRoleRequest(UserRole.VIEWER), otherId, "127.0.0.1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last active admin");
    }
}
