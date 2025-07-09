package com.proptech.realestate.model.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch document for property search
 * Phase 8 Implementation: Advanced Search with Elasticsearch
 */
@Document(indexName = "properties")
@Setting(settingPath = "/elasticsearch/property-settings.json")
@Mapping(mappingPath = "/elasticsearch/property-mapping.json")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PropertySearchDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String listingKey;

    @Field(type = FieldType.Keyword)
    private String mlsId;

    @Field(type = FieldType.Text, analyzer = "address_analyzer")
    private String address;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Keyword)
    private String zipCode;

    @Field(type = FieldType.Keyword)
    private String county;

    @Field(type = FieldType.Text)
    private String neighborhood;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Keyword)
    private String propertyType;

    @Field(type = FieldType.Keyword)
    private String propertySubType;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal listPrice;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal originalListPrice;

    @Field(type = FieldType.Integer)
    private Integer bedroomsTotal;

    @Field(type = FieldType.Integer)
    private Integer bathroomsTotalInteger;

    @Field(type = FieldType.Integer)
    private Integer bathroomsFull;

    @Field(type = FieldType.Integer)
    private Integer bathroomsHalf;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal livingArea;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal lotSizeSquareFeet;

    @Field(type = FieldType.Integer)
    private Integer yearBuilt;

    @Field(type = FieldType.Integer)
    private Integer stories;

    @Field(type = FieldType.Integer)
    private Integer parkingSpaces;

    @Field(type = FieldType.Keyword)
    private String standardStatus;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String publicRemarks;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String privateRemarks;

    @Field(type = FieldType.Keyword, index = false)
    private List<String> photos;

    @Field(type = FieldType.Nested)
    private List<PropertyFeature> features;

    @Field(type = FieldType.Date)
    private LocalDateTime listingContractDate;

    @Field(type = FieldType.Date)
    private LocalDateTime modificationTimestamp;

    @Field(type = FieldType.Date)
    private LocalDateTime photoModificationTimestamp;

    @Field(type = FieldType.Integer)
    private Integer daysOnMarket;

    @Field(type = FieldType.Integer)
    private Integer cumulativeDaysOnMarket;

    // Agent Information
    @Field(type = FieldType.Keyword)
    private String listAgentMlsId;

    @Field(type = FieldType.Text)
    private String listAgentFullName;

    @Field(type = FieldType.Keyword)
    private String listAgentEmail;

    @Field(type = FieldType.Keyword)
    private String listAgentPhone;

    // Office Information
    @Field(type = FieldType.Keyword)
    private String listOfficeMlsId;

    @Field(type = FieldType.Text)
    private String listOfficeName;

    @Field(type = FieldType.Keyword)
    private String listOfficePhone;

    // School Information
    @Field(type = FieldType.Text)
    private String schoolDistrict;

    @Field(type = FieldType.Keyword)
    private String elementarySchool;

    @Field(type = FieldType.Keyword)
    private String middleSchool;

    @Field(type = FieldType.Keyword)
    private String highSchool;

    // Financial Information
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal taxAnnualAmount;

    @Field(type = FieldType.Integer)
    private Integer taxYear;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal hoaFee;

    @Field(type = FieldType.Keyword)
    private String hoaFeeFrequency;

    // Property Details
    @Field(type = FieldType.Keyword)
    private String architecturalStyle;

    @Field(type = FieldType.Keyword)
    private String constructionMaterials;

    @Field(type = FieldType.Keyword)
    private String heating;

    @Field(type = FieldType.Keyword)
    private String cooling;

    @Field(type = FieldType.Keyword)
    private String waterSource;

    @Field(type = FieldType.Keyword)
    private String sewer;

    // Search Optimization
    @Field(type = FieldType.Text, analyzer = "search_analyzer")
    private String searchText; // Combined searchable text

    @Field(type = FieldType.Keyword)
    private List<String> searchTags; // Tags for faceted search

    // Pricing and Market Data
    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal pricePerSquareFoot;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 100)
    private BigDecimal estimatedValue; // AVM value

    @Field(type = FieldType.Scaled_Float, scalingFactor = 10)
    private BigDecimal walkScore;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 10)
    private BigDecimal transitScore;

    @Field(type = FieldType.Scaled_Float, scalingFactor = 10)
    private BigDecimal bikeScore;

    // Virtual Tour
    @Field(type = FieldType.Keyword, index = false)
    private String virtualTourUrl;

    @Field(type = FieldType.Boolean)
    private Boolean hasVirtualTour;

    // Search Metadata
    @Field(type = FieldType.Date)
    private LocalDateTime indexedAt;

    @Field(type = FieldType.Integer)
    private Integer searchScore; // Custom relevance score

    @Field(type = FieldType.Object)
    private Map<String, Object> customFields; // Flexible additional data

    /**
     * Calculate price per square foot
     */
    public BigDecimal calculatePricePerSquareFoot() {
        if (listPrice != null && livingArea != null && livingArea.compareTo(BigDecimal.ZERO) > 0) {
            return listPrice.divide(livingArea, 2, BigDecimal.ROUND_HALF_UP);
        }
        return null;
    }

    /**
     * Build search text from multiple fields
     */
    public String buildSearchText() {
        StringBuilder sb = new StringBuilder();
        
        if (address != null) sb.append(address).append(" ");
        if (city != null) sb.append(city).append(" ");
        if (state != null) sb.append(state).append(" ");
        if (zipCode != null) sb.append(zipCode).append(" ");
        if (neighborhood != null) sb.append(neighborhood).append(" ");
        if (schoolDistrict != null) sb.append(schoolDistrict).append(" ");
        if (publicRemarks != null) sb.append(publicRemarks).append(" ");
        
        return sb.toString().trim();
    }

    /**
     * Check if property has photos
     */
    public boolean hasPhotos() {
        return photos != null && !photos.isEmpty();
    }

    /**
     * Get photo count
     */
    public int getPhotoCount() {
        return photos != null ? photos.size() : 0;
    }

    /**
     * Check if property is active
     */
    public boolean isActive() {
        return "Active".equalsIgnoreCase(standardStatus);
    }
}
