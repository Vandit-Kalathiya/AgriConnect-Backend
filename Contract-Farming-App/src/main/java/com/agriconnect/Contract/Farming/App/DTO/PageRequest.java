package com.agriconnect.Contract.Farming.App.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    
    @Builder.Default
    private Integer limit = 20; // Default page size
    
    private String cursor; // Cursor for pagination
    
    @Builder.Default
    private String sortDirection = "DESC"; // ASC or DESC
    
    // Validate limit
    public Integer getLimit() {
        if (limit == null || limit < 1) {
            return 20;
        }
        // Max 100 records per page
        return Math.min(limit, 100);
    }
    
    public String getSortDirection() {
        if (sortDirection == null) {
            return "DESC";
        }
        return sortDirection.toUpperCase();
    }
}
