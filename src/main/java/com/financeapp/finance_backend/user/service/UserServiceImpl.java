package com.financeapp.finance_backend.user.service;

import com.financeapp.finance_backend.audit.service.AuditService;
import com.financeapp.finance_backend.common.exception.BusinessRuleException;
import com.financeapp.finance_backend.common.exception.DuplicateResourceException;
import com.financeapp.finance_backend.common.exception.ResourceNotFoundException;
import com.financeapp.finance_backend.user.dto.*;
import com.financeapp.finance_backend.user.entity.User;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import com.financeapp.finance_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe(UUID currentUserId) {
        return userRepository.findById(currentUserId)
                .filter(u -> u.getDeletedAt() == null)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(UserRole role, UserStatus status, Pageable pageable) {
        if (role != null && status != null) {
            return userRepository.findByRoleAndStatusAndDeletedAtIsNull(role, status, pageable).map(this::toResponse);
        } else if (role != null) {
            return userRepository.findByRoleAndDeletedAtIsNull(role, pageable).map(this::toResponse);
        } else if (status != null) {
            return userRepository.findByStatusAndDeletedAtIsNull(status, pageable).map(this::toResponse);
        }
        return userRepository.findByDeletedAtIsNull(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request, UUID currentUserId, String ipAddress) {
        if (userRepository.existsByEmailAndDeletedAtIsNull(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(request.role())
                .status(UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        auditService.log("USER", user.getId(), "CREATE", currentUserId, null, toResponse(user), ipAddress);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(UUID id, UpdateStatusRequest request, UUID currentUserId, String ipAddress) {
        User user = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Prevent deactivating the last active admin
        if (UserStatus.INACTIVE.equals(request.status()) && UserRole.ADMIN.equals(user.getRole())) {
            long activeAdmins = userRepository.countActiveAdmins();
            if (activeAdmins <= 1) {
                throw new BusinessRuleException("Cannot deactivate the last active admin user");
            }
        }

        UserResponse oldState = toResponse(user);
        user.setStatus(request.status());
        user = userRepository.save(user);

        auditService.log("USER", user.getId(), "STATUS_CHANGE", currentUserId, oldState, toResponse(user), ipAddress);
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateRole(UUID id, UpdateRoleRequest request, UUID currentUserId, String ipAddress) {
        // Self-demotion prevention
        if (id.equals(currentUserId)) {
            throw new BusinessRuleException("Admins cannot change their own role");
        }

        User user = userRepository.findById(id)
                .filter(u -> u.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        // Guard last admin demotion
        if (UserRole.ADMIN.equals(user.getRole()) && !UserRole.ADMIN.equals(request.role())) {
            long activeAdmins = userRepository.countActiveAdmins();
            if (activeAdmins <= 1) {
                throw new BusinessRuleException("Cannot demote the last active admin user");
            }
        }

        UserResponse oldState = toResponse(user);
        user.setRole(request.role());
        user = userRepository.save(user);

        auditService.log("USER", user.getId(), "ROLE_CHANGE", currentUserId, oldState, toResponse(user), ipAddress);
        return toResponse(user);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFullName(),
                user.getRole(), user.getStatus(),
                user.getCreatedAt(), user.getUpdatedAt(), user.getVersion()
        );
    }
}
