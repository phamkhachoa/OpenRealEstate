package com.proptech.realestate.service.idx.impl;

import com.proptech.realestate.dto.idx.IdxFeedResult;
import com.proptech.realestate.model.dto.reso.PropertyResource;
import com.proptech.realestate.model.entity.IdxFeed;
import com.proptech.realestate.model.entity.IdxFeedType;
import com.proptech.realestate.model.entity.Property;
import com.proptech.realestate.service.PropertyService;
import com.proptech.realestate.service.ResoMappingService;
import com.proptech.realestate.service.idx.IdxFeedProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RESO Web API IDX Feed Processor
 * Phase 7 Implementation
 */
@Component
@Slf4j
public class ResoIdxFeedProcessor implements IdxFeedProcessor {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ResoMappingService mappingService;

    @Autowired
    private PropertyService propertyService;

    private static final String PROCESSOR_NAME = "RESO Web API Processor";
    private static final DateTimeFormatter RESO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    @Override
    public IdxFeedResult processFeed(IdxFeed feed) {
        log.info("Processing RESO feed: {} ({})", feed.getMlsName(), feed.getMlsId());
        
        IdxFeedResult result = IdxFeedResult.builder()
            .totalProcessed(0)
            .totalCreated(0)
            .totalUpdated(0)
            .build();

        long startTime = System.currentTimeMillis();

        try {
            // Build RESO API URL with filters
            String apiUrl = buildResoApiUrl(feed);
            log.debug("RESO API URL: {}", apiUrl);

            // Create HTTP headers with authentication
            HttpHeaders headers = createAuthHeaders(feed);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Make API request
            ResponseEntity<ResoPropertyResponse> response = restTemplate.exchange(
                apiUrl, HttpMethod.GET, entity, ResoPropertyResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<PropertyResource> resoProperties = response.getBody().getValue();
                log.info("Retrieved {} properties from RESO API", resoProperties.size());

                // Process each property
                for (PropertyResource resoProperty : resoProperties) {
                    try {
                        processResoProperty(resoProperty, feed, result);
                    } catch (Exception e) {
                        log.error("Failed to process property: {}", resoProperty.getListingKey(), e);
                        result.addError("Failed to process property " + resoProperty.getListingKey() + ": " + e.getMessage());
                    }
                }

                // Handle pagination if needed
                if (response.getBody().getOdataNextLink() != null) {
                    log.info("Processing additional pages...");
                    processAdditionalPages(response.getBody().getOdataNextLink(), headers, feed, result);
                }

            } else {
                throw new RuntimeException("Failed to retrieve data from RESO API. Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Error processing RESO feed: {}", feed.getMlsName(), e);
            result.addError("Feed processing failed: " + e.getMessage());
        }

        long endTime = System.currentTimeMillis();
        result.setProcessingTimeMs(endTime - startTime);
        result.calculateProcessingRate();

        log.info("RESO feed processing completed: {}", result.getSummary());
        return result;
    }

    /**
     * Process individual RESO property
     */
    private void processResoProperty(PropertyResource resoProperty, IdxFeed feed, IdxFeedResult result) {
        try {
            // Map RESO property to internal Property entity
            Property property = mappingService.mapResoToProperty(resoProperty, feed.getMlsId());
            
            if (property == null) {
                result.addWarning("Skipped null property mapping for ListingKey: " + resoProperty.getListingKey());
                result.incrementSkipped();
                return;
            }

            // Check if property already exists
            Property existingProperty = propertyService.findByListingKey(resoProperty.getListingKey());
            
            if (existingProperty != null) {
                // Update existing property
                updateExistingProperty(existingProperty, property);
                Property savedProperty = propertyService.save(existingProperty);
                result.addProcessedProperty(savedProperty);
                result.incrementUpdated();
                log.debug("Updated existing property: {}", resoProperty.getListingKey());
            } else {
                // Create new property
                Property savedProperty = propertyService.save(property);
                result.addProcessedProperty(savedProperty);
                result.incrementCreated();
                log.debug("Created new property: {}", resoProperty.getListingKey());
            }

        } catch (Exception e) {
            log.error("Failed to process RESO property: {}", resoProperty.getListingKey(), e);
            result.addError("Property " + resoProperty.getListingKey() + ": " + e.getMessage());
            result.incrementSkipped();
        }
    }

    /**
     * Update existing property with new data
     */
    private void updateExistingProperty(Property existing, Property updated) {
        // Update only if the new data is more recent
        if (updated.getModificationTimestamp() != null && 
            (existing.getModificationTimestamp() == null || 
             updated.getModificationTimestamp().isAfter(existing.getModificationTimestamp()))) {
            
            // Copy updated fields
            existing.setListPrice(updated.getListPrice());
            existing.setStandardStatus(updated.getStandardStatus());
            existing.setPublicRemarks(updated.getPublicRemarks());
            existing.setModificationTimestamp(updated.getModificationTimestamp());
            existing.setDaysOnMarket(updated.getDaysOnMarket());
            
            // Update other relevant fields
            if (updated.getBedroomsTotal() != null) {
                existing.setBedroomsTotal(updated.getBedroomsTotal());
            }
            if (updated.getBathroomsTotalInteger() != null) {
                existing.setBathroomsTotalInteger(updated.getBathroomsTotalInteger());
            }
            if (updated.getLivingArea() != null) {
                existing.setLivingArea(updated.getLivingArea());
            }
        }
    }

    /**
     * Process additional pages from paginated response
     */
    private void processAdditionalPages(String nextLink, HttpHeaders headers, IdxFeed feed, IdxFeedResult result) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<ResoPropertyResponse> response = restTemplate.exchange(
                nextLink, HttpMethod.GET, entity, ResoPropertyResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<PropertyResource> properties = response.getBody().getValue();
                
                for (PropertyResource property : properties) {
                    processResoProperty(property, feed, result);
                }

                // Continue with next page if available
                if (response.getBody().getOdataNextLink() != null) {
                    processAdditionalPages(response.getBody().getOdataNextLink(), headers, feed, result);
                }
            }
        } catch (Exception e) {
            log.error("Error processing additional pages", e);
            result.addError("Failed to process additional pages: " + e.getMessage());
        }
    }

    /**
     * Build RESO API URL with filters
     */
    private String buildResoApiUrl(IdxFeed feed) {
        StringBuilder url = new StringBuilder(feed.getFeedUrl());
        
        if (!feed.getFeedUrl().endsWith("/")) {
            url.append("/");
        }
        url.append("Property");

        // Add filters
        List<String> filters = new ArrayList<>();
        
        // Only get active listings by default
        filters.add("StandardStatus eq 'Active'");
        
        // Add timestamp filter for incremental sync
        if (feed.getLastSyncTimestamp() != null) {
            String timestamp = feed.getLastSyncTimestamp().format(RESO_DATE_FORMAT);
            filters.add("ModificationTimestamp gt " + timestamp);
        }

        // Add configuration-based filters
        if (feed.getFeedConfiguration() != null) {
            Map<String, Object> config = feed.getFeedConfiguration();
            
            if (config.containsKey("propertyTypes")) {
                @SuppressWarnings("unchecked")
                List<String> propertyTypes = (List<String>) config.get("propertyTypes");
                if (!propertyTypes.isEmpty()) {
                    String typeFilter = propertyTypes.stream()
                        .map(type -> "'" + type + "'")
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                    filters.add("PropertyType in (" + typeFilter + ")");
                }
            }
            
            if (config.containsKey("maxPrice")) {
                filters.add("ListPrice le " + config.get("maxPrice"));
            }
        }

        // Add query parameters
        if (!filters.isEmpty()) {
            url.append("?$filter=").append(String.join(" and ", filters));
            url.append("&$orderby=ModificationTimestamp desc");
            url.append("&$top=1000"); // Limit per page
        } else {
            url.append("?$orderby=ModificationTimestamp desc&$top=1000");
        }

        return url.toString();
    }

    /**
     * Create authentication headers
     */
    private HttpHeaders createAuthHeaders(IdxFeed feed) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");
        headers.set("User-Agent", "OpenRealEstate-IDX/1.0");

        switch (feed.getAuthType()) {
            case BASIC_AUTH:
                if (feed.getUsername() != null && feed.getPassword() != null) {
                    String auth = feed.getUsername() + ":" + feed.getPassword();
                    String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
                    headers.set("Authorization", "Basic " + encodedAuth);
                }
                break;
                
            case API_KEY:
                if (feed.getApiKey() != null) {
                    headers.set("Authorization", "Bearer " + feed.getApiKey());
                }
                break;
                
            case BEARER_TOKEN:
                if (feed.getApiKey() != null) {
                    headers.set("Authorization", "Bearer " + feed.getApiKey());
                }
                break;
                
            case OAUTH2:
                // OAuth2 implementation would go here
                // This would require token management
                break;
                
            default:
                // No authentication
                break;
        }

        return headers;
    }

    @Override
    public boolean supports(String feedType) {
        return IdxFeedType.RESO_WEB_API.name().equals(feedType);
    }

    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }

    @Override
    public boolean validateFeedConfiguration(IdxFeed feed) {
        if (feed.getFeedUrl() == null || feed.getFeedUrl().trim().isEmpty()) {
            return false;
        }

        // Validate authentication configuration
        switch (feed.getAuthType()) {
            case BASIC_AUTH:
                return feed.getUsername() != null && feed.getPassword() != null;
            case API_KEY:
            case BEARER_TOKEN:
                return feed.getApiKey() != null;
            case OAUTH2:
                return feed.getClientId() != null && feed.getClientSecret() != null;
            case NONE:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean testConnection(IdxFeed feed) {
        try {
            String testUrl = feed.getFeedUrl();
            if (!testUrl.endsWith("/")) {
                testUrl += "/";
            }
            testUrl += "$metadata";

            HttpHeaders headers = createAuthHeaders(feed);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                testUrl, HttpMethod.GET, entity, String.class);

            return response.getStatusCode() == HttpStatus.OK;
            
        } catch (Exception e) {
            log.error("Connection test failed for feed: {}", feed.getMlsName(), e);
            return false;
        }
    }

    @Override
    public Long getEstimatedProcessingTime(IdxFeed feed) {
        // Estimate based on feed configuration and historical data
        // This is a simple estimation - could be more sophisticated
        if (feed.getTotalPropertiesSynced() != null && feed.getTotalPropertiesSynced() > 0) {
            // Estimate ~100 properties per second
            return (feed.getTotalPropertiesSynced() / 100) * 1000L;
        }
        return 60000L; // Default 1 minute estimate
    }

    /**
     * RESO API Response wrapper
     */
    public static class ResoPropertyResponse {
        private List<PropertyResource> value;
        private String odataNextLink;
        private String odataContext;

        // Getters and setters
        public List<PropertyResource> getValue() { return value; }
        public void setValue(List<PropertyResource> value) { this.value = value; }
        public String getOdataNextLink() { return odataNextLink; }
        public void setOdataNextLink(String odataNextLink) { this.odataNextLink = odataNextLink; }
        public String getOdataContext() { return odataContext; }
        public void setOdataContext(String odataContext) { this.odataContext = odataContext; }
    }
}
