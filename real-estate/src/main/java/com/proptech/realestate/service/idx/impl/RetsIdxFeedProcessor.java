package com.proptech.realestate.service.idx.impl;

import com.proptech.realestate.dto.idx.IdxFeedResult;
import com.proptech.realestate.model.entity.IdxFeed;
import com.proptech.realestate.model.entity.IdxFeedType;
import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.service.PropertyService;
import com.proptech.realestate.service.idx.IdxFeedProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * RETS IDX Feed Processor
 * Handles RETS (Real Estate Transaction Standard) feeds
 * Phase 7 Implementation
 */
@Component
@Slf4j
public class RetsIdxFeedProcessor implements IdxFeedProcessor {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PROCESSOR_NAME = "RETS Processor";
    private static final DateTimeFormatter RETS_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public boolean supports(IdxFeedType feedType) {
        return feedType == IdxFeedType.RETS;
    }

    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }

    @Override
    public IdxFeedResult processFeed(IdxFeed feed) {
        log.info("Starting RETS feed processing for: {}", feed.getMlsName());

        IdxFeedResult result = new IdxFeedResult();
        result.setFeedId(feed.getId());
        result.setProcessorName(PROCESSOR_NAME);
        result.setStartTime(LocalDateTime.now());

        try {
            // Step 1: Authenticate with RETS server
            RetsSession session = authenticateRetsSession(feed);
            if (session == null) {
                result.addError("Failed to authenticate with RETS server");
                result.setSuccessful(false);
                return result;
            }

            // Step 2: Get metadata to understand available resources
            Map<String, String> metadata = getRetsMetadata(session, feed);
            
            // Step 3: Search for properties
            List<Map<String, String>> properties = searchRetsProperties(session, feed, metadata);
            
            if (properties.isEmpty()) {
                result.addWarning("No properties found in RETS search");
                result.setSuccessful(true);
                return result;
            }

            // Step 4: Process each property
            for (Map<String, String> retsProperty : properties) {
                try {
                    processRetsProperty(retsProperty, feed, result);
                } catch (Exception e) {
                    log.error("Failed to process RETS property: {}", e.getMessage());
                    result.addError("Property processing failed: " + e.getMessage());
                    result.incrementSkipped();
                }
            }

            // Step 5: Logout from RETS session
            logoutRetsSession(session);

            result.setSuccessful(true);
            log.info("Completed RETS feed processing for: {}, processed: {}, created: {}, updated: {}",
                    feed.getMlsName(), result.getTotalProcessed(), result.getTotalCreated(), result.getTotalUpdated());

        } catch (Exception e) {
            log.error("Failed to process RETS feed: {}", feed.getMlsName(), e);
            result.addError("RETS processing failed: " + e.getMessage());
            result.setSuccessful(false);
        } finally {
            result.setEndTime(LocalDateTime.now());
            result.calculateDuration();
        }

        return result;
    }

    @Override
    public boolean testConnection(IdxFeed feed) {
        try {
            RetsSession session = authenticateRetsSession(feed);
            if (session != null) {
                logoutRetsSession(session);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("RETS connection test failed for: {}", feed.getMlsName(), e);
            return false;
        }
    }

    @Override
    public Long getEstimatedProcessingTime(IdxFeed feed) {
        // RETS processing can be time-consuming due to authentication and multiple requests
        long baseTime = 30000L; // 30 seconds base time
        
        // Add time based on expected data volume
        Map<String, Object> config = feed.getFeedConfiguration();
        if (config != null && config.containsKey("expectedRecords")) {
            try {
                int expectedRecords = (Integer) config.get("expectedRecords");
                // Add 1 second per 100 records
                baseTime += (expectedRecords / 100) * 1000L;
            } catch (Exception e) {
                log.debug("Could not parse expected records from feed configuration");
            }
        }

        return baseTime;
    }

    /**
     * Authenticate with RETS server
     */
    private RetsSession authenticateRetsSession(IdxFeed feed) {
        try {
            String loginUrl = feed.getFeedUrl();
            if (!loginUrl.endsWith("/Login")) {
                loginUrl += "/Login";
            }

            // Extract credentials from feed configuration
            Map<String, Object> config = feed.getFeedConfiguration();
            String username = (String) config.get("username");
            String password = (String) config.get("password");
            String userAgent = (String) config.getOrDefault("userAgent", "OpenRealEstate/1.0");

            // Perform RETS login
            String loginResponse = performRetsLogin(loginUrl, username, password, userAgent);
            
            if (loginResponse != null && loginResponse.contains("SUCCESS")) {
                return parseRetsLoginResponse(loginResponse);
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to authenticate RETS session", e);
            return null;
        }
    }

    /**
     * Perform RETS login request
     */
    private String performRetsLogin(String loginUrl, String username, String password, String userAgent) {
        try {
            // Create HTTP headers for RETS authentication
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("User-Agent", userAgent);
            headers.set("RETS-Version", "RETS/1.7.2");
            
            // Create basic auth header
            String auth = username + ":" + password;
            String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            // Make the login request
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    loginUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
            
            return response.getBody();

        } catch (Exception e) {
            log.error("RETS login request failed", e);
            return null;
        }
    }

    /**
     * Parse RETS login response
     */
    private RetsSession parseRetsLoginResponse(String loginResponse) {
        try {
            RetsSession session = new RetsSession();
            
            // Parse XML response
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(loginResponse)));
            
            Element retsElement = document.getDocumentElement();
            
            // Extract RETS capabilities URLs
            NodeList tagList = retsElement.getElementsByTagName("RETS-RESPONSE");
            if (tagList.getLength() > 0) {
                Element retsResponse = (Element) tagList.item(0);
                
                session.setSearchUrl(getElementText(retsResponse, "Search"));
                session.setGetMetadataUrl(getElementText(retsResponse, "GetMetadata"));
                session.setGetObjectUrl(getElementText(retsResponse, "GetObject"));
                session.setLogoutUrl(getElementText(retsResponse, "Logout"));
                
                session.setSessionId(getElementText(retsResponse, "SessionID"));
                session.setActive(true);
            }
            
            return session;

        } catch (Exception e) {
            log.error("Failed to parse RETS login response", e);
            return null;
        }
    }

    /**
     * Get RETS metadata
     */
    private Map<String, String> getRetsMetadata(RetsSession session, IdxFeed feed) {
        Map<String, String> metadata = new HashMap<>();
        
        try {
            String metadataUrl = session.getGetMetadataUrl() + "?Type=METADATA-RESOURCE&ID=0";
            
            org.springframework.http.HttpHeaders headers = createRetsHeaders();
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    metadataUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
            
            // Parse metadata response to extract resource information
            parseRetsMetadata(response.getBody(), metadata);
            
        } catch (Exception e) {
            log.error("Failed to get RETS metadata", e);
        }
        
        return metadata;
    }

    /**
     * Search for properties in RETS
     */
    private List<Map<String, String>> searchRetsProperties(RetsSession session, IdxFeed feed, Map<String, String> metadata) {
        List<Map<String, String>> properties = new ArrayList<>();
        
        try {
            String searchUrl = session.getSearchUrl();
            
            // Build search query
            StringBuilder query = new StringBuilder();
            query.append("?SearchType=Property");
            query.append("&Class=").append(getPropertyClass(metadata, feed));
            query.append("&Query=").append(buildRetsQuery(feed));
            query.append("&Format=COMPACT-DECODED");
            query.append("&Limit=").append(getRetsLimit(feed));
            
            String fullUrl = searchUrl + query.toString();
            
            org.springframework.http.HttpHeaders headers = createRetsHeaders();
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl, org.springframework.http.HttpMethod.GET, entity, String.class);
            
            // Parse search results
            parseRetsSearchResults(response.getBody(), properties);
            
        } catch (Exception e) {
            log.error("Failed to search RETS properties", e);
        }
        
        return properties;
    }

    /**
     * Process individual RETS property
     */
    private void processRetsProperty(Map<String, String> retsProperty, IdxFeed feed, IdxFeedResult result) {
        try {
            Listing listing = new Listing();
            
            // Map RETS fields to Listing entity
            mapRetsFieldsToListing(retsProperty, listing, feed);
            
            if (listing.getListingId() == null || listing.getListingId().trim().isEmpty()) {
                result.addWarning("Skipping property with missing listing ID");
                result.incrementSkipped();
                return;
            }

            // Check if property already exists
            Listing existingListing = propertyService.findByListingKey(listing.getListingId());
            
            if (existingListing != null) {
                // Update existing property
                updateExistingListing(existingListing, listing);
                Listing savedListing = propertyService.save(existingListing);
                result.addProcessedProperty(savedListing);
                result.incrementUpdated();
                log.debug("Updated existing listing: {}", listing.getListingId());
            } else {
                // Create new property
                Listing savedListing = propertyService.save(listing);
                result.addProcessedProperty(savedListing);
                result.incrementCreated();
                log.debug("Created new listing: {}", listing.getListingId());
            }

        } catch (Exception e) {
            log.error("Failed to process RETS property: {}", e.getMessage());
            result.addError("RETS property processing failed: " + e.getMessage());
            result.incrementSkipped();
        }
    }

    /**
     * Map RETS fields to Listing entity
     */
    private void mapRetsFieldsToListing(Map<String, String> retsProperty, Listing listing, IdxFeed feed) {
        listing.setMlsId(feed.getMlsId());
        
        // Map standard RETS fields
        listing.setListingId(retsProperty.get("ListingID"));
        listing.setPropertyType(retsProperty.get("PropertyType"));
        listing.setUnparsedAddress(retsProperty.get("StreetName"));
        listing.setCity(retsProperty.get("City"));
        listing.setStateOrProvince(retsProperty.get("StateOrProvince"));
        listing.setPostalCode(retsProperty.get("PostalCode"));
        listing.setPublicRemarks(retsProperty.get("PublicRemarks"));

        // Parse numeric fields safely
        parseAndSetPrice(retsProperty.get("ListPrice"), listing::setListPrice);
        parseAndSetInteger(retsProperty.get("BedroomsTotal"), listing::setBedroomsTotal);
        parseAndSetInteger(retsProperty.get("BathroomsTotal"), listing::setBathroomsTotalInteger);
        parseAndSetDouble(retsProperty.get("LivingArea"), listing::setLivingArea);
        parseAndSetDouble(retsProperty.get("Latitude"), listing::setLatitude);
        parseAndSetDouble(retsProperty.get("Longitude"), listing::setLongitude);
        parseAndSetInteger(retsProperty.get("YearBuilt"), listing::setYearBuilt);

        // Map status
        String status = retsProperty.get("StandardStatus");
        if (status != null) {
            listing.setStandardStatus(mapRetsStatus(status));
        }

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (listing.getCreatedAt() == null) {
            listing.setCreatedAt(now);
        }
        listing.setUpdatedAt(now);
    }

    // Helper methods
    private org.springframework.http.HttpHeaders createRetsHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("User-Agent", "OpenRealEstate/1.0");
        headers.set("RETS-Version", "RETS/1.7.2");
        return headers;
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }

    private void parseRetsMetadata(String metadataXml, Map<String, String> metadata) {
        // Parse metadata XML to extract resource classes and field mappings
        // Implementation would depend on specific RETS server metadata format
    }

    private void parseRetsSearchResults(String searchXml, List<Map<String, String>> properties) {
        // Parse RETS search results XML
        // Implementation would depend on specific RETS response format
    }

    private String getPropertyClass(Map<String, String> metadata, IdxFeed feed) {
        // Get appropriate property class from metadata
        return "RES"; // Default residential class
    }

    private String buildRetsQuery(IdxFeed feed) {
        // Build RETS query based on feed configuration
        Map<String, Object> config = feed.getFeedConfiguration();
        
        List<String> conditions = new ArrayList<>();
        conditions.add("(StandardStatus=|Active)");
        
        if (config != null) {
            if (config.containsKey("minPrice")) {
                conditions.add("(ListPrice=" + config.get("minPrice") + "+)");
            }
            if (config.containsKey("maxPrice")) {
                conditions.add("(ListPrice=" + config.get("maxPrice") + "-)");
            }
        }
        
        return String.join(",", conditions);
    }

    private String getRetsLimit(IdxFeed feed) {
        Map<String, Object> config = feed.getFeedConfiguration();
        if (config != null && config.containsKey("batchSize")) {
            return config.get("batchSize").toString();
        }
        return "1000"; // Default limit
    }

    private void parseAndSetPrice(String value, java.util.function.Consumer<java.math.BigDecimal> setter) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(java.math.BigDecimal.valueOf(Double.parseDouble(value)));
            } catch (NumberFormatException e) {
                log.warn("Invalid price value: {}", value);
            }
        }
    }

    private void parseAndSetInteger(String value, java.util.function.Consumer<Integer> setter) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value: {}", value);
            }
        }
    }

    private void parseAndSetDouble(String value, java.util.function.Consumer<Double> setter) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid double value: {}", value);
            }
        }
    }

    private com.proptech.realestate.model.enums.StandardStatus mapRetsStatus(String retsStatus) {
        if (retsStatus == null) return com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
        
        String status = retsStatus.toLowerCase().trim();
        return switch (status) {
            case "active" -> com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
            case "pending" -> com.proptech.realestate.model.enums.StandardStatus.PENDING;
            case "sold", "closed" -> com.proptech.realestate.model.enums.StandardStatus.SOLD;
            case "cancelled", "canceled" -> com.proptech.realestate.model.enums.StandardStatus.CANCELLED;
            case "expired" -> com.proptech.realestate.model.enums.StandardStatus.EXPIRED;
            case "withdrawn" -> com.proptech.realestate.model.enums.StandardStatus.WITHDRAWN;
            default -> com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
        };
    }

    private void updateExistingListing(Listing existing, Listing updated) {
        // Update key fields from RETS data
        if (updated.getListPrice() != null) {
            existing.setListPrice(updated.getListPrice());
        }
        if (updated.getStandardStatus() != null) {
            existing.setStandardStatus(updated.getStandardStatus());
        }
        if (updated.getPublicRemarks() != null) {
            existing.setPublicRemarks(updated.getPublicRemarks());
        }
        if (updated.getBedroomsTotal() != null) {
            existing.setBedroomsTotal(updated.getBedroomsTotal());
        }
        if (updated.getBathroomsTotalInteger() != null) {
            existing.setBathroomsTotalInteger(updated.getBathroomsTotalInteger());
        }
        if (updated.getLivingArea() != null) {
            existing.setLivingArea(updated.getLivingArea());
        }
        
        existing.setUpdatedAt(LocalDateTime.now());
    }

    private void logoutRetsSession(RetsSession session) {
        try {
            if (session != null && session.isActive() && session.getLogoutUrl() != null) {
                org.springframework.http.HttpHeaders headers = createRetsHeaders();
                org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
                
                restTemplate.exchange(session.getLogoutUrl(), org.springframework.http.HttpMethod.GET, entity, String.class);
                session.setActive(false);
            }
        } catch (Exception e) {
            log.warn("Failed to logout from RETS session", e);
        }
    }

    /**
     * RETS Session holder
     */
    @lombok.Data
    private static class RetsSession {
        private String sessionId;
        private String searchUrl;
        private String getMetadataUrl;
        private String getObjectUrl;
        private String logoutUrl;
        private boolean active;
    }
}
