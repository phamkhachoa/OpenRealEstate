package com.proptech.realestate.model.dto;

import com.proptech.realestate.model.entity.Listing;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchListingResponse {
    
    private List<ListingSearchResult> results; // Renamed from listings for consistency
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private String searchType; // DATABASE, ELASTICSEARCH, SMART_ELASTICSEARCH
    private Double nlpConfidence; // NLP confidence score if applicable
    private SearchMetadata metadata; // Additional metadata
    
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
        private String propertyType;
        private String status;
        private String address;
        private String fullAddress; // Full formatted address
        private String categoryName;
        private LocalDateTime createdAt;
        private String updatedAt;
        
        // Elasticsearch-specific fields
        private Double score; // Relevance score from Elasticsearch
        private Map<String, List<String>> highlights; // Highlighted text snippets
        
        // Enhanced fields
        private String thumbnailUrl;
        private Double pricePerSqm;
        private List<String> amenities; // Extracted amenities
        private String locationInfo; // Formatted location info
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
        private String searchEngine; // "DATABASE", "ELASTICSEARCH"
        private Map<String, Object> aggregations; // Elasticsearch aggregations
        private Map<String, Integer> facets; // Search facets
    }
    
    // Helper methods
    public static SearchListingResponse empty() {
        return SearchListingResponse.builder()
            .results(List.of())
            .totalElements(0)
            .totalPages(0)
            .currentPage(0)
            .pageSize(0)
            .searchType("EMPTY")
            .build();
    }
    
    // Backward compatibility
    public List<ListingSearchResult> getListings() {
        return results;
    }
    
    public void setListings(List<ListingSearchResult> listings) {
        this.results = listings;
    }
    
    public long getTotalElements() {
        return totalElements;
    }
    
    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }
    
    public boolean hasPrevious() {
        return currentPage > 0;
    }
} 