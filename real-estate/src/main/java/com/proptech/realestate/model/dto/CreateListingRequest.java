package com.proptech.realestate.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class CreateListingRequest {
    private String userId;
    private String title;
    private String publicRemarks; // RESO: PublicRemarks
    private String unparsedAddress; // RESO: UnparsedAddress
    private BigDecimal listPrice; // RESO: ListPrice
    private Double livingArea; // RESO: LivingArea
    private Integer bedroomsTotal; // RESO: BedroomsTotal
    private Integer bathroomsTotalInteger; // RESO: BathroomsTotalInteger
    private String propertyType; // RESO: PropertyType (e.g., "SingleFamilyResidence")
    private Double latitude;
    private Double longitude;
    private Map<String, Object> additionalDetails; // For other RESO fields
} 
