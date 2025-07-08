package com.proptech.realestate.model.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Document(indexName = "real_estate_listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String listingCode;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String fullAddress;

    // Geospatial data
    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Text, analyzer = "keyword")
    private String city;

    @Field(type = FieldType.Text, analyzer = "keyword")
    private String district;

    @Field(type = FieldType.Text, analyzer = "keyword")
    private String ward;

    // Numeric fields for range queries
    @Field(type = FieldType.Double)
    private BigDecimal price;

    @Field(type = FieldType.Double)
    private Double area;

    @Field(type = FieldType.Double)
    private Double pricePerSqm;

    @Field(type = FieldType.Integer)
    private Integer numBedrooms;

    @Field(type = FieldType.Integer)
    private Integer numBathrooms;

    // Category and type
    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Text, analyzer = "keyword")
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String listingType; // RENT, SALE, etc.

    @Field(type = FieldType.Keyword)
    private String status; // ACTIVE, INACTIVE, etc.

    // User information
    @Field(type = FieldType.Keyword)
    private String userId;

    @Field(type = FieldType.Text, analyzer = "keyword")
    private String userDisplayName;

    @Field(type = FieldType.Keyword)
    private String userType; // INDIVIDUAL, AGENT, DEVELOPER

    // Timestamps
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;

    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;

    // Dynamic attributes as nested objects
    @Field(type = FieldType.Nested)
    private List<DynamicAttribute> dynamicAttributes;

    // Search boost fields
    @Field(type = FieldType.Text, analyzer = "standard")
    private String searchText; // Combined text for full-text search

    @Field(type = FieldType.Keyword)
    private List<String> tags; // SEO tags, features

    // Statistics for ranking
    @Field(type = FieldType.Integer)
    private Integer viewCount = 0;

    @Field(type = FieldType.Integer)
    private Integer favoriteCount = 0;

    @Field(type = FieldType.Integer)
    private Integer contactCount = 0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DynamicAttribute {
        @Field(type = FieldType.Keyword)
        private String name;

        @Field(type = FieldType.Keyword)
        private String value;

        @Field(type = FieldType.Keyword)
        private String type; // STRING, NUMBER, BOOLEAN, etc.
    }

    // Utility methods for search text
    public void buildSearchText() {
        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append(title).append(" ");
        if (description != null) sb.append(description).append(" ");
        if (fullAddress != null) sb.append(fullAddress).append(" ");
        if (categoryName != null) sb.append(categoryName).append(" ");
        if (city != null) sb.append(city).append(" ");
        if (district != null) sb.append(district).append(" ");
        if (ward != null) sb.append(ward).append(" ");
        
        this.searchText = sb.toString().trim();
    }

    // Calculate price per square meter
    public void calculatePricePerSqm() {
        if (price != null && area != null && area > 0) {
            this.pricePerSqm = price.doubleValue() / area;
        }
    }
} 