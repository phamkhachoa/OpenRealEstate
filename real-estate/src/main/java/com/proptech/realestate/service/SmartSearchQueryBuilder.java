package com.proptech.realestate.service;

import com.proptech.realestate.model.dto.NLPQueryResult;
import com.proptech.realestate.model.dto.SearchListingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Service to convert NLP results into structured search queries
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SmartSearchQueryBuilder {

    /**
     * Build SearchListingRequest from NLP processing results
     */
    public SearchListingRequest buildFromNLP(NLPQueryResult nlpResult) {
        if (nlpResult == null || nlpResult.getExtractedEntities() == null) {
            log.warn("No extracted entities found in NLP result");
            return new SearchListingRequest();
        }

        SearchListingRequest request = new SearchListingRequest();
        NLPQueryResult.ExtractedEntities entities = nlpResult.getExtractedEntities();

        // Set property type
        if (entities.getPropertyType() != null) {
            request.setPropertyType(entities.getPropertyType());
            log.debug("Set property type: {}", entities.getPropertyType());
        }

        // Set price range
        if (entities.getPriceRange() != null) {
            setPriceRange(request, entities.getPriceRange());
        }

        // Set area range  
        if (entities.getAreaRange() != null) {
            setAreaRange(request, entities.getAreaRange());
        }

        // Set bedrooms
        if (entities.getBedrooms() != null) {
            request.setBedroomsTotal(entities.getBedrooms());
            log.debug("Set bedrooms: {}", entities.getBedrooms());
        }

        // Set bathrooms
        if (entities.getBathrooms() != null) {
            request.setBathroomsTotalInteger(entities.getBathrooms());
            log.debug("Set bathrooms: {}", entities.getBathrooms());
        }

        // Set location
        if (entities.getLocation() != null) {
            setLocationFilters(request, entities.getLocation());
        }

        // Set amenities/keywords for search
        if (entities.getAmenities() != null && !entities.getAmenities().isEmpty()) {
            setAmenitiesSearch(request, entities.getAmenities());
        }

        // Set transaction type as a note or filter if supported
        if (entities.getTransactionType() != null) {
            log.debug("Transaction type detected: {}", entities.getTransactionType());
            // Could be used for filtering buy/rent listings if supported
        }

        // Set original query for reference
        request.setQuery(nlpResult.getOriginalQuery());

        log.info("Built search request from NLP: propertyType={}, bedrooms={}, priceRange=({}-{})", 
            request.getPropertyType(), request.getBedroomsTotal(), 
            request.getMinPrice(), request.getMaxPrice());

        return request;
    }

    /**
     * Set price range filters based on NLP extraction
     */
    private void setPriceRange(SearchListingRequest request, NLPQueryResult.PriceRange priceRange) {
        String operator = priceRange.getOperator();
        
        if ("lt".equals(operator) && priceRange.getMaxPrice() != null) {
            // "dưới X tỷ"
            request.setMaxPrice(BigDecimal.valueOf(priceRange.getMaxPrice()));
            log.debug("Set max price: {}", priceRange.getMaxPrice());
            
        } else if ("gt".equals(operator) && priceRange.getMinPrice() != null) {
            // "trên X tỷ"
            request.setMinPrice(BigDecimal.valueOf(priceRange.getMinPrice()));
            log.debug("Set min price: {}", priceRange.getMinPrice());
            
        } else if ("range".equals(operator)) {
            // "từ X đến Y tỷ"
            if (priceRange.getMinPrice() != null) {
                request.setMinPrice(BigDecimal.valueOf(priceRange.getMinPrice()));
            }
            if (priceRange.getMaxPrice() != null) {
                request.setMaxPrice(BigDecimal.valueOf(priceRange.getMaxPrice()));
            }
            log.debug("Set price range: {} - {}", priceRange.getMinPrice(), priceRange.getMaxPrice());
            
        } else if ("eq".equals(operator) && priceRange.getExactPrice() != null) {
            // "X tỷ" - set as approximate range (±20%)
            long exactPrice = priceRange.getExactPrice();
            long minPrice = (long) (exactPrice * 0.8);
            long maxPrice = (long) (exactPrice * 1.2);
            
            request.setMinPrice(BigDecimal.valueOf(minPrice));
            request.setMaxPrice(BigDecimal.valueOf(maxPrice));
            log.debug("Set exact price as range: {} (±20%)", exactPrice);
        }
    }

    /**
     * Set area range filters based on NLP extraction
     */
    private void setAreaRange(SearchListingRequest request, NLPQueryResult.AreaRange areaRange) {
        String operator = areaRange.getOperator();
        
        if ("lt".equals(operator) && areaRange.getMaxArea() != null) {
            request.setMaxArea(BigDecimal.valueOf(areaRange.getMaxArea()));
            log.debug("Set max area: {}", areaRange.getMaxArea());
            
        } else if ("gt".equals(operator) && areaRange.getMinArea() != null) {
            request.setMinArea(BigDecimal.valueOf(areaRange.getMinArea()));
            log.debug("Set min area: {}", areaRange.getMinArea());
            
        } else if ("range".equals(operator)) {
            if (areaRange.getMinArea() != null) {
                request.setMinArea(BigDecimal.valueOf(areaRange.getMinArea()));
            }
            if (areaRange.getMaxArea() != null) {
                request.setMaxArea(BigDecimal.valueOf(areaRange.getMaxArea()));
            }
            log.debug("Set area range: {} - {}", areaRange.getMinArea(), areaRange.getMaxArea());
            
        } else if ("eq".equals(operator) && areaRange.getExactArea() != null) {
            // Exact area - set as approximate range (±15%)
            double exactArea = areaRange.getExactArea();
            double minArea = exactArea * 0.85;
            double maxArea = exactArea * 1.15;
            
            request.setMinArea(BigDecimal.valueOf(minArea));
            request.setMaxArea(BigDecimal.valueOf(maxArea));
            log.debug("Set exact area as range: {} (±15%)", exactArea);
        }
    }

    /**
     * Set location filters based on NLP extraction
     */
    private void setLocationFilters(SearchListingRequest request, NLPQueryResult.LocationInfo location) {
        List<String> locationKeywords = new ArrayList<>();
        
        if (location.getDistrict() != null) {
            locationKeywords.add(location.getDistrict());
            log.debug("Added district to location search: {}", location.getDistrict());
        }
        
        if (location.getWard() != null) {
            locationKeywords.add(location.getWard());
            log.debug("Added ward to location search: {}", location.getWard());
        }
        
        if (location.getArea() != null) {
            locationKeywords.add(location.getArea());
            log.debug("Added area to location search: {}", location.getArea());
        }
        
        if (location.getNerLocation() != null) {
            locationKeywords.add(location.getNerLocation());
            log.debug("Added NER location to search: {}", location.getNerLocation());
        }
        
        // Combine location keywords into search query
        if (!locationKeywords.isEmpty()) {
            String existingQuery = request.getQuery();
            String locationQuery = String.join(" ", locationKeywords);
            
            if (existingQuery != null && !existingQuery.trim().isEmpty()) {
                request.setQuery(existingQuery + " " + locationQuery);
            } else {
                request.setQuery(locationQuery);
            }
        }
    }

    /**
     * Set amenities as search keywords
     */
    private void setAmenitiesSearch(SearchListingRequest request, List<String> amenities) {
        List<String> amenityKeywords = new ArrayList<>();
        
        for (String amenity : amenities) {
            // Convert amenity codes to searchable keywords
            String keyword = convertAmenityToKeyword(amenity);
            if (keyword != null) {
                amenityKeywords.add(keyword);
            }
        }
        
        if (!amenityKeywords.isEmpty()) {
            String existingQuery = request.getQuery();
            String amenityQuery = String.join(" ", amenityKeywords);
            
            if (existingQuery != null && !existingQuery.trim().isEmpty()) {
                request.setQuery(existingQuery + " " + amenityQuery);
            } else {
                request.setQuery(amenityQuery);
            }
            
            log.debug("Added amenities to search: {}", amenityKeywords);
        }
    }

    /**
     * Convert amenity codes to searchable keywords
     */
    private String convertAmenityToKeyword(String amenityCode) {
        switch (amenityCode) {
            case "near_school": return "trường học";
            case "near_hospital": return "bệnh viện";
            case "near_market": return "chợ";
            case "near_supermarket": return "siêu thị";
            case "near_park": return "công viên";
            case "river_view": return "view sông";
            case "ocean_view": return "view biển";
            case "swimming_pool": return "hồ bơi";
            case "gym": return "gym";
            case "security": return "bảo vệ";
            case "elevator": return "thang máy";
            case "parking": return "chỗ đậu xe";
            case "garage": return "gara";
            case "garden": return "sân vườn";
            case "balcony": return "ban công";
            default: return null;
        }
    }

    /**
     * Validate and adjust search parameters for better results
     */
    public SearchListingRequest validateAndOptimize(SearchListingRequest request) {
        // Ensure reasonable price ranges
        if (request.getMinPrice() != null && request.getMaxPrice() != null) {
            if (request.getMinPrice().compareTo(request.getMaxPrice()) > 0) {
                // Swap if min > max
                BigDecimal temp = request.getMinPrice();
                request.setMinPrice(request.getMaxPrice());
                request.setMaxPrice(temp);
                log.debug("Swapped min/max price for logical range");
            }
        }

        // Ensure reasonable area ranges
        if (request.getMinArea() != null && request.getMaxArea() != null) {
            if (request.getMinArea().compareTo(request.getMaxArea()) > 0) {
                // Swap if min > max
                BigDecimal temp = request.getMinArea();
                request.setMinArea(request.getMaxArea());
                request.setMaxArea(temp);
                log.debug("Swapped min/max area for logical range");
            }
        }

        // Set reasonable defaults for missing parameters
        if (request.getPage() == null) {
            request.setPage(0);
        }
        
        if (request.getSize() == null) {
            request.setSize(20);
        }

        return request;
    }
} 