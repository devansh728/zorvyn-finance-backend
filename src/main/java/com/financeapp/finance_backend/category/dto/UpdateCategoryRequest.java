package com.financeapp.finance_backend.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload to update a category name")
public record UpdateCategoryRequest(
	@Schema(description = "Updated category name", example = "Housing")
	@NotBlank @Size(max = 100) String name) {}
