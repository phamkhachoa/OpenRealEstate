package com.proptech.realestate.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchListingRequest {
    
    // Basic search parameters
    private String keyword;
    private String location;
    private String listingType; // RENT, SALE, etc.
    private String status; // ACTIVE, etc.
    
    // Price range
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    
    // Area range
    private Double minArea;
    private Double maxArea;
    
    // Property specifics
    private Integer minBedrooms;
    private Integer maxBedrooms;
    private Integer minBathrooms;
    private Integer maxBathrooms;
    private String propertyType; // APARTMENT, HOUSE, VILLA, OFFICE
    
    // Category
    private String categoryId;
    
    // Dynamic attributes search
    private Map<String, String> dynamicAttributes;
    
    // Sorting and pagination
    private String sortBy; // price, area, created_at, etc.
    private String sortDirection; // ASC, DESC
    private Integer page;
    private Integer size;
    
    // Geospatial search (future enhancement)
    private Double latitude;
    private Double longitude;
    private Double radius;
} 