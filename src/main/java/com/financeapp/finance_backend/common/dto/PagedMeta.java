package com.financeapp.finance_backend.common.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PagedMeta {
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
