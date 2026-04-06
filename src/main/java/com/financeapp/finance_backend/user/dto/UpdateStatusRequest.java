package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to update a user's status")
public record UpdateStatusRequest(
	@Schema(description = "New account status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
	@NotNull UserStatus status) {}
