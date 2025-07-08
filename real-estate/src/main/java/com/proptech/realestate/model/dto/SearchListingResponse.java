package com.proptech.realestate.model.dto;

import com.proptech.realestate.model.entity.Listing;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchListingResponse {
    
    private List<ListingSearchResult> listings;
    private SearchMetadata metadata;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingSearchResult {
        private String id;
        private String listingCode;
        private String title;
        private String description;
        private String price;
        private String area;
        private Integer numBedrooms;
        private Integer numBathrooms;
        private String listingType;
        private String status;
        private String address;
        private String categoryName;
        private LocalDateTime createdAt;
        private String updatedAt;
        
        // Thumbnail image URL (future enhancement)
        private String thumbnailUrl;
        
        // Price per square meter for comparison
        private Double pricePerSqm;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMetadata {
        private long totalCount;
        private int totalPages;
        private int currentPage;
        private int pageSize;
        private boolean hasNext;
        private boolean hasPrevious;
        private String sortBy;
        private String sortDirection;
        private long searchTimeMs;
    }
} 