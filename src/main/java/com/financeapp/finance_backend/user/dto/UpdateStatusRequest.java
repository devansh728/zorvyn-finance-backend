package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull UserStatus status) {}
