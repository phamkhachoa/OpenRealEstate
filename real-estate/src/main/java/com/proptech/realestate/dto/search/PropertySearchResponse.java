package com.proptech.realestate.dto.search;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Property search response DTO
 * Phase 8 Implementation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PropertySearchResponse {

    private List<PropertySearchResult> results;
    private Long totalResults;
    private Integer page;
    private Integer size;
    private Long took; // Query execution time in milliseconds
    private String searchId;
    private String scrollId; // For pagination with large result sets

    // Aggregations (faceted search)
    private Map<String, Object> aggregations;

    // Search metadata
    private LocalDateTime timestamp;
    private Boolean timedOut;
    private Double maxScore;

    // Suggestions
    private List<SearchSuggestion> suggestions;

    // Market statistics (optional)
    private MarketStatistics marketStats;

    /**
     * Check if search has results
     */
    public boolean hasResults() {
        return results != null && !results.isEmpty();
    }

    /**
     * Get result count
     */
    public Integer getResultCount() {
        return results != null ? results.size() : 0;
    }

    /**
     * Check if there are more pages
     */
    public boolean hasNextPage() {
        if (totalResults == null || size == null || page == null) {
            return false;
        }
        
        long totalPages = (totalResults + size - 1) / size;
        return page + 1 < totalPages;
    }

    /**
     * Check if there are previous pages
     */
    public boolean hasPreviousPage() {
        return page != null && page > 0;
    }

    /**
     * Get total pages
     */
    public Long getTotalPages() {
        if (totalResults == null || size == null || size == 0) {
            return 0L;
        }
        return (totalResults + size - 1) / size;
    }

    /**
     * Check if search was fast (< 100ms)
     */
    public boolean isFastSearch() {
        return took != null && took < 100;
    }

    /**
     * Get search performance rating
     */
    public String getPerformanceRating() {
        if (took == null) return "Unknown";
        if (took < 50) return "Excellent";
        if (took < 100) return "Good";
        if (took < 200) return "Fair";
        return "Slow";
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketStatistics {
        private Long totalListings;
        private Long activeListings;
        private Double averagePrice;
        private Double medianPrice;
        private Double minPrice;
        private Double maxPrice;
        private Double averageDaysOnMarket;
        private Double averageSquareFeet;
        private Map<String, Long> propertyTypeCounts;
        private Map<String, Double> priceRangeDistribution;
    }
}
