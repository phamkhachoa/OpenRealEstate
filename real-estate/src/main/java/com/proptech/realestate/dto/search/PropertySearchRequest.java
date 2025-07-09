package com.proptech.realestate.dto.search;

import lombok.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Property search request DTO
 * Phase 8 Implementation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PropertySearchRequest {

    // Pagination
    @Builder.Default
    private Integer page = 0;
    
    @Builder.Default
    private Integer size = 20;

    // Geographic search
    private GeoBounds bounds;
    private GeoPoint centerPoint;
    private Double radiusMiles;
    private String city;
    private String state;
    private List<String> zipCodes;
    private String county;
    private String neighborhood;

    // Text search
    private String query;

    // Property filters
    private List<String> propertyTypes;
    private List<String> propertySubTypes;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Integer minBedrooms;
    private Integer maxBedrooms;
    private Integer minBathrooms;
    private Integer maxBathrooms;
    private BigDecimal minSquareFeet;
    private BigDecimal maxSquareFeet;
    private BigDecimal minLotSize;
    private BigDecimal maxLotSize;
    private Integer minYearBuilt;
    private Integer maxYearBuilt;
    private Integer minParkingSpaces;

    // Status filters
    private List<String> statusFilter;

    // Property features
    private List<String> requiredFeatures;
    private List<String> excludedFeatures;

    // School filters
    private String schoolDistrict;
    private String elementarySchool;
    private String middleSchool;
    private String highSchool;

    // Agent/Office filters
    private String agentMlsId;
    private String officeMlsId;

    // Date filters
    private String listedSince; // "1d", "7d", "30d", "90d"
    private String updatedSince;
    private Integer maxDaysOnMarket;

    // Financial filters
    private BigDecimal maxHoaFee;
    private BigDecimal maxTaxes;

    // Amenity filters
    private Boolean hasPool;
    private Boolean hasFireplace;
    private Boolean hasGarage;
    private Boolean hasBasement;
    private Boolean hasPhotos;
    private Boolean hasVirtualTour;

    // Sorting
    @Builder.Default
    private String sortBy = "_score"; // "_score", "listPrice", "daysOnMarket", "modificationTimestamp"
    
    @Builder.Default
    private String sortOrder = "desc"; // "asc", "desc"

    // Advanced options
    private Boolean includePhotos = false;
    private Boolean includeFeatures = true;
    private Boolean includePriceHistory = false;
    private Boolean includeMarketStats = false;

    // User context (for personalization)
    private String userId;
    private Map<String, Object> userPreferences;

    // Search analytics
    private String searchId;
    private String sessionId;
    private Map<String, Object> analyticsContext;

    /**
     * Validate search request
     */
    public boolean isValid() {
        // Check pagination limits
        if (size > 1000) {
            return false;
        }

        // Check geographic consistency
        if (bounds != null && centerPoint != null) {
            return false; // Can't have both bounds and center point
        }

        // Check price range
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            return false;
        }

        // Check bedroom/bathroom ranges
        if (minBedrooms != null && maxBedrooms != null && minBedrooms > maxBedrooms) {
            return false;
        }

        if (minBathrooms != null && maxBathrooms != null && minBathrooms > maxBathrooms) {
            return false;
        }

        return true;
    }

    /**
     * Check if this is a geographic search
     */
    public boolean hasGeographicFilter() {
        return bounds != null || centerPoint != null || 
               city != null || state != null || 
               (zipCodes != null && !zipCodes.isEmpty()) ||
               county != null || neighborhood != null;
    }

    /**
     * Check if this is a text search
     */
    public boolean hasTextSearch() {
        return query != null && !query.trim().isEmpty();
    }

    /**
     * Check if this has property filters
     */
    public boolean hasPropertyFilters() {
        return (propertyTypes != null && !propertyTypes.isEmpty()) ||
               minPrice != null || maxPrice != null ||
               minBedrooms != null || maxBedrooms != null ||
               minBathrooms != null || maxBathrooms != null ||
               minSquareFeet != null || maxSquareFeet != null;
    }

    /**
     * Get effective page size (with max limit)
     */
    public Integer getEffectiveSize() {
        return Math.min(size, 1000);
    }

    /**
     * Check if sorting by price
     */
    public boolean isSortingByPrice() {
        return "listPrice".equals(sortBy);
    }

    /**
     * Check if sorting by relevance
     */
    public boolean isSortingByRelevance() {
        return "_score".equals(sortBy);
    }

    /**
     * Check if sorting by date
     */
    public boolean isSortingByDate() {
        return "modificationTimestamp".equals(sortBy) || "listingContractDate".equals(sortBy);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GeoBounds {
        private GeoPoint northEast;
        private GeoPoint southWest;

        /**
         * Check if bounds are valid
         */
        public boolean isValid() {
            return northEast != null && southWest != null &&
                   northEast.getLat() > southWest.getLat() &&
                   northEast.getLon() > southWest.getLon();
        }

        /**
         * Get center point of bounds
         */
        public GeoPoint getCenter() {
            if (!isValid()) {
                return null;
            }
            
            double centerLat = (northEast.getLat() + southWest.getLat()) / 2.0;
            double centerLon = (northEast.getLon() + southWest.getLon()) / 2.0;
            
            return new GeoPoint(centerLat, centerLon);
        }
    }
}
