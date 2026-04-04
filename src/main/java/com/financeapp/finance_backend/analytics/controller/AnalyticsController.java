package com.financeapp.finance_backend.analytics.controller;

import com.financeapp.finance_backend.analytics.dto.*;
import com.financeapp.finance_backend.analytics.service.AnalyticsService;
import com.financeapp.finance_backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@Tag(name = "Analytics", description = "Financial analytics and reporting")
@RestController
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @Operation(summary = "Get financial summary (all roles)")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getSummary(startDate, endDate), "Summary retrieved"));
    }

    @Operation(summary = "Get category breakdown (all roles)")
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryBreakdownResponse>>> getCategoryBreakdown(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getCategoryBreakdown(type, startDate, endDate), "Category breakdown retrieved"));
    }

    @Operation(summary = "Get trend data (all roles)")
    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<List<TrendDataPoint>>> getTrends(
            @RequestParam(defaultValue = "MONTHLY") TrendInterval interval,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        return ResponseEntity.ok(ApiResponse.success(
                analyticsService.getTrends(interval, startDate, endDate), "Trends retrieved"));
    }
}
