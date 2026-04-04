package com.financeapp.finance_backend.user.service;

import com.financeapp.finance_backend.user.dto.*;
import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    UserResponse getMe(UUID currentUserId);

    Page<UserResponse> listUsers(UserRole role, UserStatus status, Pageable pageable);

    UserResponse getUserById(UUID id);

    UserResponse createUser(CreateUserRequest request, UUID currentUserId, String ipAddress);

    UserResponse updateStatus(UUID id, UpdateStatusRequest request, UUID currentUserId, String ipAddress);

    UserResponse updateRole(UUID id, UpdateRoleRequest request, UUID currentUserId, String ipAddress);
}
