package com.financeapp.finance_backend.record.dto;

import com.financeapp.finance_backend.record.entity.TransactionType;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;
import java.util.UUID;

public record RecordFilterCriteria(
        TransactionType type,
        UUID categoryId,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        String search
) {}
