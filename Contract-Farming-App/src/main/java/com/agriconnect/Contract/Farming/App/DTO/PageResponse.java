package com.agriconnect.Contract.Farming.App.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    
    private List<T> data;
    private PageMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PageMetadata {
        private String nextCursor;
        private String prevCursor;
        private boolean hasNext;
        private boolean hasPrev;
        private int pageSize;
        private int returnedCount;
    }
}
