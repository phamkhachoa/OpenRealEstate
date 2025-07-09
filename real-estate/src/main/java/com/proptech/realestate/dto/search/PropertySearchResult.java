package com.proptech.realestate.dto.search;

import lombok.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Individual property search result
 * Phase 8 Implementation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PropertySearchResult {

    // Basic property information
    private String id;
    private String listingKey;
    private String mlsId;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String neighborhood;
    private GeoPoint location;

    // Property details
    private String propertyType;
    private String propertySubType;
    private BigDecimal listPrice;
    private Integer bedroomsTotal;
    private Integer bathroomsTotalInteger;
    private BigDecimal livingArea;
    private BigDecimal lotSizeSquareFeet;
    private Integer yearBuilt;
    private String standardStatus;

    // Search metadata
    private Float score; // Elasticsearch relevance score
    private Integer searchRank;
    private List<String> highlights; // Highlighted text matches
    private String snippet; // Short description snippet

    // Display information
    private String primaryPhoto;
    private Integer photoCount;
    private Boolean hasVirtualTour;
    private String publicRemarks;

    // Agent/Office information
    private String listAgentName;
    private String listOfficeName;
    private String listAgentPhone;

    // Market data
    private Integer daysOnMarket;
    private BigDecimal pricePerSquareFoot;
    private BigDecimal estimatedValue;
    private String priceChangeIndicator; // "UP", "DOWN", "SAME"

    // Location information
    private String schoolDistrict;
    private Double walkScore;
    private Double transitScore;

    // Computed fields
    private LocalDateTime listingDate;
    private LocalDateTime lastModified;
    private Boolean isPriceReduced;
    private Boolean isNewListing;
    private Boolean isFeatured;

    // Additional data (optional)
    private Map<String, Object> features;
    private Map<String, Object> additionalData;

    /**
     * Get formatted price string
     */
    public String getFormattedPrice() {
        if (listPrice == null) {
            return "Price upon request";
        }
        
        if (listPrice.compareTo(BigDecimal.valueOf(1000000)) >= 0) {
            return String.format("$%.1fM", listPrice.divide(BigDecimal.valueOf(1000000)).doubleValue());
        } else if (listPrice.compareTo(BigDecimal.valueOf(1000)) >= 0) {
            return String.format("$%.0fK", listPrice.divide(BigDecimal.valueOf(1000)).doubleValue());
        } else {
            return String.format("$%,.0f", listPrice.doubleValue());
        }
    }

    /**
     * Get property description summary
     */
    public String getPropertySummary() {
        StringBuilder summary = new StringBuilder();
        
        if (bedroomsTotal != null && bedroomsTotal > 0) {
            summary.append(bedroomsTotal).append(" bed");
            if (bedroomsTotal > 1) summary.append("s");
        }
        
        if (bathroomsTotalInteger != null && bathroomsTotalInteger > 0) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(bathroomsTotalInteger).append(" bath");
            if (bathroomsTotalInteger > 1) summary.append("s");
        }
        
        if (livingArea != null && livingArea.compareTo(BigDecimal.ZERO) > 0) {
            if (summary.length() > 0) summary.append(", ");
            summary.append(String.format("%,.0f sqft", livingArea.doubleValue()));
        }
        
        return summary.toString();
    }

    /**
     * Get formatted address
     */
    public String getFormattedAddress() {
        StringBuilder addr = new StringBuilder();
        
        if (address != null) {
            addr.append(address);
        }
        
        if (city != null) {
            if (addr.length() > 0) addr.append(", ");
            addr.append(city);
        }
        
        if (state != null) {
            if (addr.length() > 0) addr.append(", ");
            addr.append(state);
        }
        
        if (zipCode != null) {
            if (addr.length() > 0) addr.append(" ");
            addr.append(zipCode);
        }
        
        return addr.toString();
    }

    /**
     * Check if property is recently listed
     */
    public boolean isRecentListing() {
        if (listingDate == null) {
            return false;
        }
        return listingDate.isAfter(LocalDateTime.now().minusDays(7));
    }

    /**
     * Check if property is in luxury price range
     */
    public boolean isLuxuryProperty() {
        return listPrice != null && listPrice.compareTo(BigDecimal.valueOf(1000000)) >= 0;
    }

    /**
     * Check if property has good value (price per sqft)
     */
    public boolean hasGoodValue() {
        // This would typically compare against local market averages
        // For now, just check if price per sqft is reasonable
        return pricePerSquareFoot != null && pricePerSquareFoot.compareTo(BigDecimal.valueOf(500)) <= 0;
    }

    /**
     * Get relevance score as percentage
     */
    public Double getRelevancePercentage() {
        if (score == null) {
            return null;
        }
        // Normalize score to 0-100 range (this would depend on your scoring strategy)
        return Math.min(100.0, score * 10.0);
    }

    /**
     * Get property value indicator
     */
    public String getValueIndicator() {
        if (estimatedValue != null && listPrice != null) {
            double ratio = listPrice.divide(estimatedValue, 4, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (ratio < 0.95) return "GOOD_VALUE";
            if (ratio > 1.05) return "OVERPRICED";
            return "FAIR_VALUE";
        }
        return "UNKNOWN";
    }
}
