package com.financeapp.finance_backend.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiResponse<T> {
    @Schema(description = "Indicates whether the request was processed successfully", example = "true")
    private boolean success;

    @Schema(description = "Human-readable operation result message", example = "Operation successful")
    private String message;

    @Schema(description = "Payload of the response. Structure depends on endpoint")
    private T data;

    @Schema(description = "Pagination metadata for paged responses")
    private PagedMeta meta;

    @Schema(description = "Error details when success is false")
    private ErrorDetail error;

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data) {
        return success(data, "Operation successful");
    }

    public static <T> ApiResponse<T> paged(T data, PagedMeta meta, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .meta(meta)
                .build();
    }

    public static <T> ApiResponse<T> error(ErrorDetail errorDetail, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(errorDetail)
                .build();
    }
}
