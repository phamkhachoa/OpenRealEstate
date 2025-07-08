package com.proptech.realestate.model.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class CreateListingRequest {

    private String title;
    private String description;
    private BigDecimal price;
    private Double area;
    private Integer numBedrooms;
    private Integer numBathrooms;

    private String userId;
    private Integer categoryId;
    
    // Simplified address for now
    private String fullAddress;

    // Key: attribute code (e.g., "balcony_direction")
    // Value: attribute value (e.g., "Đông Nam")
    private Map<String, String> attributes;
} 
