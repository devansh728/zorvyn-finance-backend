package com.financeapp.finance_backend.user.dto;

import com.financeapp.finance_backend.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request payload to update a user's role")
public record UpdateRoleRequest(
	@Schema(description = "New role for the user", example = "ANALYST", allowableValues = {"VIEWER", "ANALYST", "ADMIN"})
	@NotNull UserRole role) {}
