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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * XML IDX Feed Processor
 * Handles generic XML-based IDX feeds
 * Phase 7 Implementation
 */
@Component
@Slf4j
public class XmlIdxFeedProcessor implements IdxFeedProcessor {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PROCESSOR_NAME = "XML Feed Processor";
    private static final DateTimeFormatter XML_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Override
    public boolean supports(IdxFeedType feedType) {
        return feedType == IdxFeedType.XML;
    }

    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }

    @Override
    public IdxFeedResult processFeed(IdxFeed feed) {
        log.info("Starting XML feed processing for: {}", feed.getMlsName());

        IdxFeedResult result = new IdxFeedResult();
        result.setFeedId(feed.getId());
        result.setProcessorName(PROCESSOR_NAME);
        result.setStartTime(LocalDateTime.now());

        try {
            // Load XML document
            Document xmlDocument = loadXmlDocument(feed);
            
            if (xmlDocument == null) {
                result.addError("Failed to load XML document");
                result.setSuccessful(false);
                return result;
            }

            // Extract property elements from XML
            List<Element> propertyElements = extractPropertyElements(xmlDocument, feed);
            
            if (propertyElements.isEmpty()) {
                result.addWarning("No property elements found in XML document");
                result.setSuccessful(true);
                return result;
            }

            // Process each property element
            for (Element propertyElement : propertyElements) {
                try {
                    processXmlProperty(propertyElement, feed, result);
                } catch (Exception e) {
                    log.error("Failed to process XML property element: {}", e.getMessage());
                    result.addError("Property processing failed: " + e.getMessage());
                    result.incrementSkipped();
                }
            }

            result.setSuccessful(true);
            log.info("Completed XML feed processing for: {}, processed: {}, created: {}, updated: {}",
                    feed.getMlsName(), result.getTotalProcessed(), result.getTotalCreated(), result.getTotalUpdated());

        } catch (Exception e) {
            log.error("Failed to process XML feed: {}", feed.getMlsName(), e);
            result.addError("XML processing failed: " + e.getMessage());
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
            Document xmlDocument = loadXmlDocument(feed);
            return xmlDocument != null;
        } catch (Exception e) {
            log.error("XML feed connection test failed for: {}", feed.getMlsName(), e);
            return false;
        }
    }

    @Override
    public Long getEstimatedProcessingTime(IdxFeed feed) {
        // Base processing time for XML files
        long baseTime = 10000L; // 10 seconds

        // Estimate based on file size if available
        try {
            long fileSize = getXmlFileSize(feed.getFeedUrl());
            if (fileSize > 0) {
                // Estimate 2 seconds per MB (XML parsing is more intensive)
                return baseTime + (fileSize / (1024 * 1024)) * 2000L;
            }
        } catch (Exception e) {
            log.debug("Could not estimate file size for XML feed: {}", feed.getFeedUrl());
        }

        return baseTime;
    }

    /**
     * Load XML document from URL or file path
     */
    private Document loadXmlDocument(IdxFeed feed) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();

            if (feed.getFeedUrl().startsWith("http")) {
                // Load from HTTP URL
                String xmlContent = restTemplate.getForObject(feed.getFeedUrl(), String.class);
                if (xmlContent != null) {
                    return builder.parse(new ByteArrayInputStream(xmlContent.getBytes()));
                }
            } else {
                // Load from local file
                File xmlFile = new File(feed.getFeedUrl());
                if (xmlFile.exists()) {
                    return builder.parse(new FileInputStream(xmlFile));
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Failed to load XML document: {}", feed.getFeedUrl(), e);
            return null;
        }
    }

    /**
     * Extract property elements from XML document
     */
    private List<Element> extractPropertyElements(Document xmlDocument, IdxFeed feed) {
        List<Element> propertyElements = new ArrayList<>();

        try {
            // Get property element name from feed configuration or use default
            String propertyElementName = getPropertyElementName(feed);
            
            NodeList propertyNodes = xmlDocument.getElementsByTagName(propertyElementName);
            
            for (int i = 0; i < propertyNodes.getLength(); i++) {
                Node node = propertyNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    propertyElements.add((Element) node);
                }
            }

        } catch (Exception e) {
            log.error("Failed to extract property elements from XML", e);
        }

        return propertyElements;
    }

    /**
     * Process individual XML property element
     */
    private void processXmlProperty(Element propertyElement, IdxFeed feed, IdxFeedResult result) {
        try {
            Listing listing = new Listing();
            
            // Map XML fields to Listing entity
            mapXmlFieldsToListing(propertyElement, listing, feed);
            
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
            log.error("Failed to process XML property: {}", e.getMessage());
            result.addError("XML property processing failed: " + e.getMessage());
            result.incrementSkipped();
        }
    }

    /**
     * Map XML fields to Listing entity
     */
    private void mapXmlFieldsToListing(Element propertyElement, Listing listing, IdxFeed feed) {
        listing.setMlsId(feed.getMlsId());
        
        // Get field mappings from feed configuration
        Map<String, String> fieldMappings = getXmlFieldMappings(feed);
        
        // Map basic string fields
        listing.setListingId(getElementTextByMapping(propertyElement, "listingId", fieldMappings));
        listing.setPropertyType(getElementTextByMapping(propertyElement, "propertyType", fieldMappings));
        listing.setUnparsedAddress(getElementTextByMapping(propertyElement, "address", fieldMappings));
        listing.setCity(getElementTextByMapping(propertyElement, "city", fieldMappings));
        listing.setStateOrProvince(getElementTextByMapping(propertyElement, "state", fieldMappings));
        listing.setPostalCode(getElementTextByMapping(propertyElement, "postalCode", fieldMappings));
        listing.setPublicRemarks(getElementTextByMapping(propertyElement, "description", fieldMappings));

        // Map numeric fields with safe parsing
        parseAndSetPrice(getElementTextByMapping(propertyElement, "listPrice", fieldMappings), listing::setListPrice);
        parseAndSetInteger(getElementTextByMapping(propertyElement, "bedrooms", fieldMappings), listing::setBedroomsTotal);
        parseAndSetInteger(getElementTextByMapping(propertyElement, "bathrooms", fieldMappings), listing::setBathroomsTotalInteger);
        parseAndSetDouble(getElementTextByMapping(propertyElement, "livingArea", fieldMappings), listing::setLivingArea);
        parseAndSetDouble(getElementTextByMapping(propertyElement, "latitude", fieldMappings), listing::setLatitude);
        parseAndSetDouble(getElementTextByMapping(propertyElement, "longitude", fieldMappings), listing::setLongitude);
        parseAndSetInteger(getElementTextByMapping(propertyElement, "yearBuilt", fieldMappings), listing::setYearBuilt);

        // Map status
        String status = getElementTextByMapping(propertyElement, "status", fieldMappings);
        if (status != null) {
            listing.setStandardStatus(mapXmlStatus(status));
        }

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (listing.getCreatedAt() == null) {
            listing.setCreatedAt(now);
        }
        listing.setUpdatedAt(now);
    }

    /**
     * Get property element name from feed configuration
     */
    private String getPropertyElementName(IdxFeed feed) {
        Map<String, Object> config = feed.getFeedConfiguration();
        if (config != null && config.containsKey("propertyElementName")) {
            return (String) config.get("propertyElementName");
        }
        
        // Try common property element names
        return "property"; // Default
    }

    /**
     * Get XML field mappings from feed configuration
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getXmlFieldMappings(IdxFeed feed) {
        Map<String, String> mappings = new java.util.HashMap<>();
        
        // Default mappings
        mappings.put("listingId", "ListingID,ID,ListingNumber");
        mappings.put("propertyType", "PropertyType,Type");
        mappings.put("address", "Address,StreetAddress,FullAddress");
        mappings.put("city", "City");
        mappings.put("state", "State,StateOrProvince");
        mappings.put("postalCode", "PostalCode,ZipCode,Zip");
        mappings.put("listPrice", "ListPrice,Price");
        mappings.put("bedrooms", "Bedrooms,BedroomsTotal,Beds");
        mappings.put("bathrooms", "Bathrooms,BathroomsTotal,Baths");
        mappings.put("livingArea", "LivingArea,SquareFeet,SQFT");
        mappings.put("latitude", "Latitude,Lat");
        mappings.put("longitude", "Longitude,Lng,Long");
        mappings.put("status", "Status,ListingStatus,StandardStatus");
        mappings.put("description", "Description,Remarks,PublicRemarks");
        mappings.put("yearBuilt", "YearBuilt,Year");
        
        // Override with feed-specific mappings if available
        Map<String, Object> config = feed.getFeedConfiguration();
        if (config != null && config.containsKey("fieldMappings")) {
            try {
                Map<String, String> feedMappings = (Map<String, String>) config.get("fieldMappings");
                mappings.putAll(feedMappings);
            } catch (ClassCastException e) {
                log.warn("Invalid field mappings configuration for feed: {}", feed.getMlsName());
            }
        }
        
        return mappings;
    }

    /**
     * Get element text using field mapping
     */
    private String getElementTextByMapping(Element parent, String fieldKey, Map<String, String> mappings) {
        String elementNames = mappings.get(fieldKey);
        if (elementNames == null) {
            return null;
        }
        
        String[] names = elementNames.split(",");
        for (String name : names) {
            String value = getElementText(parent, name.trim());
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        
        return null;
    }

    /**
     * Get text content of XML element
     */
    private String getElementText(Element parent, String tagName) {
        NodeList nodeList = parent.getElementsByTagName(tagName);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0);
            return node.getTextContent();
        }
        return null;
    }

    /**
     * Map XML status to StandardStatus enum
     */
    private com.proptech.realestate.model.enums.StandardStatus mapXmlStatus(String xmlStatus) {
        if (xmlStatus == null) return com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
        
        String status = xmlStatus.toLowerCase().trim();
        return switch (status) {
            case "active", "a" -> com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
            case "pending", "p", "under contract" -> com.proptech.realestate.model.enums.StandardStatus.PENDING;
            case "sold", "closed", "s", "c" -> com.proptech.realestate.model.enums.StandardStatus.SOLD;
            case "cancelled", "canceled" -> com.proptech.realestate.model.enums.StandardStatus.CANCELLED;
            case "expired", "e" -> com.proptech.realestate.model.enums.StandardStatus.EXPIRED;
            case "withdrawn", "w" -> com.proptech.realestate.model.enums.StandardStatus.WITHDRAWN;
            default -> com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
        };
    }

    /**
     * Update existing listing with new XML data
     */
    private void updateExistingListing(Listing existing, Listing updated) {
        // Update key fields from XML data
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

    // Utility methods for safe parsing
    private void parseAndSetPrice(String value, java.util.function.Consumer<java.math.BigDecimal> setter) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                // Remove any currency symbols or formatting
                String cleanValue = value.replaceAll("[^0-9.]", "");
                setter.accept(java.math.BigDecimal.valueOf(Double.parseDouble(cleanValue)));
            } catch (NumberFormatException e) {
                log.warn("Invalid price value: {}", value);
            }
        }
    }

    private void parseAndSetInteger(String value, java.util.function.Consumer<Integer> setter) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Integer.parseInt(value.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid integer value: {}", value);
            }
        }
    }

    private void parseAndSetDouble(String value, java.util.function.Consumer<Double> setter) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                setter.accept(Double.parseDouble(value.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid double value: {}", value);
            }
        }
    }

    private long getXmlFileSize(String path) {
        try {
            if (path.startsWith("http")) {
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(path).openConnection();
                connection.setRequestMethod("HEAD");
                return connection.getContentLengthLong();
            } else {
                return new File(path).length();
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
