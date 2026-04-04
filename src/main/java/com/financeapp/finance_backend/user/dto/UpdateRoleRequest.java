package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserRole;
import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull UserRole role) {}
