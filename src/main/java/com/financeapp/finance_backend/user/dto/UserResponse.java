package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserRole;
import com.financeapp.finance_backend.user.entity.UserStatus;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        UserRole role,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt,
        int version
) {}
