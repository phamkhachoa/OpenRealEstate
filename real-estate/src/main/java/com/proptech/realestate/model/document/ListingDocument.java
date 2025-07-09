package com.proptech.realestate.model.document;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Elasticsearch document for Listing entity
 * Optimized for Vietnamese real estate search
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "listings")
public class ListingDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    private String listingCode;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;
    
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal price;
    
    @Field(type = FieldType.Double)
    private Double area;
    
    @Field(type = FieldType.Integer)
    private Integer numBedrooms;
    
    @Field(type = FieldType.Integer)
    private Integer numBathrooms;
    
    @Field(type = FieldType.Keyword)
    private String propertyType;
    
    @Field(type = FieldType.Keyword)
    private String listingType;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Text, analyzer = "standard")
    private String fullAddress;
    
    @Field(type = FieldType.Keyword)
    private String district;
    
    @Field(type = FieldType.Keyword)
    private String ward;
    
    @Field(type = FieldType.Keyword)
    private String city;
    
    @Field(type = FieldType.Text)
    private List<String> amenities;
    
    @Field(type = FieldType.Date)
    private LocalDateTime createdAt;
    
    @Field(type = FieldType.Date)
    private LocalDateTime updatedAt;
    
    @Field(type = FieldType.Double)
    private Double latitude;
    
    @Field(type = FieldType.Double)
    private Double longitude;
    
    @Field(type = FieldType.Keyword)
    private String categoryName;
    
    @Field(type = FieldType.Text)
    private String ownerName;
    
    @Field(type = FieldType.Keyword)
    private String ownerPhone;
    
    @Field(type = FieldType.Boolean)
    private Boolean featured;
    
    @Field(type = FieldType.Integer)
    private Integer viewCount;
    
    @Field(type = FieldType.Double)
    private Double pricePerSqm;
    
    // Additional search-optimized fields
    @Field(type = FieldType.Text, analyzer = "keyword")
    private String searchKeywords; // Concatenated keywords for better search
    
    @Field(type = FieldType.Text)
    private String locationText; // Formatted location for search
} 