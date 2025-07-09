package com.proptech.realestate.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for NLP query processing results from Python UndertheSea
 */
@Data
public class NLPQueryResult {
    
    @JsonProperty("original_query")
    private String originalQuery;
    
    @JsonProperty("normalized_query")  
    private String normalizedQuery;
    
    private List<String> tokens;
    
    @JsonProperty("ner_results")
    private List<List<String>> nerResults;
    
    @JsonProperty("extracted_entities")
    private ExtractedEntities extractedEntities;
    
    @JsonProperty("confidence_score")
    private Double confidenceScore;
    
    private String error;
    
    @Data
    public static class ExtractedEntities {
        
        @JsonProperty("property_type")
        private String propertyType;
        
        @JsonProperty("price_range")
        private PriceRange priceRange;
        
        @JsonProperty("area_range")
        private AreaRange areaRange;
        
        private Integer bedrooms;
        
        private Integer bathrooms;
        
        private LocationInfo location;
        
        private List<String> amenities;
        
        @JsonProperty("transaction_type")
        private String transactionType;
    }
    
    @Data
    public static class PriceRange {
        @JsonProperty("min_price")
        private Long minPrice;
        
        @JsonProperty("max_price")
        private Long maxPrice;
        
        @JsonProperty("exact_price")
        private Long exactPrice;
        
        private String operator; // "lt", "gt", "eq", "range"
    }
    
    @Data
    public static class AreaRange {
        @JsonProperty("min_area")
        private Double minArea;
        
        @JsonProperty("max_area")
        private Double maxArea;
        
        @JsonProperty("exact_area")
        private Double exactArea;
        
        private String operator; // "lt", "gt", "eq", "range"
    }
    
    @Data
    public static class LocationInfo {
        @JsonProperty("ner_location")
        private String nerLocation;
        
        private String district;
        
        private String ward;
        
        private String area;
    }
} 