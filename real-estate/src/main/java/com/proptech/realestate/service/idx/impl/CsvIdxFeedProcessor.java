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

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CSV IDX Feed Processor
 * Handles CSV file-based IDX feeds
 * Phase 7 Implementation
 */
@Component
@Slf4j
public class CsvIdxFeedProcessor implements IdxFeedProcessor {

    @Autowired
    private PropertyService propertyService;

    private static final String PROCESSOR_NAME = "CSV Feed Processor";
    private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean supports(IdxFeedType feedType) {
        return feedType == IdxFeedType.CSV;
    }

    @Override
    public String getProcessorName() {
        return PROCESSOR_NAME;
    }

    @Override
    public IdxFeedResult processFeed(IdxFeed feed) {
        log.info("Starting CSV feed processing for: {}", feed.getMlsName());

        IdxFeedResult result = new IdxFeedResult();
        result.setFeedId(feed.getId());
        result.setProcessorName(PROCESSOR_NAME);
        result.setStartTime(LocalDateTime.now());

        try {
            // Download or read CSV file
            List<String[]> csvData = readCsvFile(feed);
            
            if (csvData.isEmpty()) {
                result.addWarning("No data found in CSV file");
                result.setSuccessful(true);
                return result;
            }

            // Parse CSV header to understand column mapping
            String[] headers = csvData.get(0);
            Map<String, Integer> columnMapping = createColumnMapping(headers);

            // Process each data row
            for (int i = 1; i < csvData.size(); i++) {
                try {
                    String[] row = csvData.get(i);
                    processCsvRow(row, columnMapping, feed, result);
                } catch (Exception e) {
                    log.error("Failed to process CSV row {}: {}", i, e.getMessage());
                    result.addError("Row " + i + ": " + e.getMessage());
                    result.incrementSkipped();
                }
            }

            result.setSuccessful(true);
            log.info("Completed CSV feed processing for: {}, processed: {}, created: {}, updated: {}",
                    feed.getMlsName(), result.getTotalProcessed(), result.getTotalCreated(), result.getTotalUpdated());

        } catch (Exception e) {
            log.error("Failed to process CSV feed: {}", feed.getMlsName(), e);
            result.addError("CSV processing failed: " + e.getMessage());
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
            // Test if CSV file/URL is accessible
            if (feed.getFeedUrl().startsWith("http")) {
                // Test HTTP URL accessibility
                return testHttpCsvAccess(feed.getFeedUrl());
            } else {
                // Test local file accessibility
                return testLocalCsvAccess(feed.getFeedUrl());
            }
        } catch (Exception e) {
            log.error("CSV feed connection test failed for: {}", feed.getMlsName(), e);
            return false;
        }
    }

    @Override
    public Long getEstimatedProcessingTime(IdxFeed feed) {
        // Base processing time for CSV files
        long baseTime = 5000L; // 5 seconds

        // Estimate based on file size if available
        try {
            long fileSize = getFileSize(feed.getFeedUrl());
            if (fileSize > 0) {
                // Estimate 1 second per MB
                return baseTime + (fileSize / (1024 * 1024)) * 1000L;
            }
        } catch (Exception e) {
            log.debug("Could not estimate file size for CSV feed: {}", feed.getFeedUrl());
        }

        return baseTime;
    }

    /**
     * Read CSV file from URL or local path
     */
    private List<String[]> readCsvFile(IdxFeed feed) throws IOException {
        List<String[]> data = new ArrayList<>();
        
        try (BufferedReader reader = createCsvReader(feed)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Simple CSV parsing (would need proper CSV library for production)
                String[] fields = parseCsvLine(line);
                data.add(fields);
            }
        }
        
        return data;
    }

    /**
     * Create CSV reader based on feed URL
     */
    private BufferedReader createCsvReader(IdxFeed feed) throws IOException {
        if (feed.getFeedUrl().startsWith("http")) {
            // Read from HTTP URL
            java.net.URL url = new java.net.URL(feed.getFeedUrl());
            return new BufferedReader(new InputStreamReader(url.openStream()));
        } else {
            // Read from local file
            return new BufferedReader(new FileReader(feed.getFeedUrl()));
        }
    }

    /**
     * Parse CSV line handling quoted fields and commas
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(currentField.toString().trim());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        // Add the last field
        fields.add(currentField.toString().trim());
        
        return fields.toArray(new String[0]);
    }

    /**
     * Create column mapping from CSV headers
     */
    private Map<String, Integer> createColumnMapping(String[] headers) {
        Map<String, Integer> mapping = new java.util.HashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toLowerCase().trim();
            
            // Map common CSV column names to our fields
            switch (header) {
                case "listing_id", "listingid", "mls_number" -> mapping.put("listingId", i);
                case "list_price", "listprice", "price" -> mapping.put("listPrice", i);
                case "property_type", "propertytype", "type" -> mapping.put("propertyType", i);
                case "bedrooms", "beds", "bedrooms_total" -> mapping.put("bedrooms", i);
                case "bathrooms", "baths", "bathrooms_total" -> mapping.put("bathrooms", i);
                case "living_area", "square_feet", "sqft" -> mapping.put("livingArea", i);
                case "address", "street_address", "full_address" -> mapping.put("address", i);
                case "city" -> mapping.put("city", i);
                case "state", "state_province" -> mapping.put("state", i);
                case "zip", "postal_code", "zipcode" -> mapping.put("postalCode", i);
                case "latitude", "lat" -> mapping.put("latitude", i);
                case "longitude", "lng", "lon" -> mapping.put("longitude", i);
                case "status", "listing_status" -> mapping.put("status", i);
                case "description", "remarks", "public_remarks" -> mapping.put("description", i);
                case "year_built", "yearbuilt" -> mapping.put("yearBuilt", i);
                case "modification_timestamp", "updated_date", "last_modified" -> mapping.put("modificationTimestamp", i);
            }
        }
        
        return mapping;
    }

    /**
     * Process individual CSV row
     */
    private void processCsvRow(String[] row, Map<String, Integer> columnMapping, IdxFeed feed, IdxFeedResult result) {
        try {
            Listing listing = new Listing();
            
            // Map CSV fields to Listing entity
            mapCsvFieldsToListing(row, columnMapping, listing, feed);
            
            if (listing.getListingId() == null || listing.getListingId().trim().isEmpty()) {
                result.addWarning("Skipping row with missing listing ID");
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
            log.error("Failed to process CSV row: {}", e.getMessage());
            result.addError("CSV row processing failed: " + e.getMessage());
            result.incrementSkipped();
        }
    }

    /**
     * Map CSV fields to Listing entity
     */
    private void mapCsvFieldsToListing(String[] row, Map<String, Integer> columnMapping, Listing listing, IdxFeed feed) {
        listing.setMlsId(feed.getMlsId());
        
        // Map basic fields
        setFieldFromCsv(row, columnMapping, "listingId", value -> listing.setListingId(value));
        setFieldFromCsv(row, columnMapping, "propertyType", value -> listing.setPropertyType(value));
        setFieldFromCsv(row, columnMapping, "address", value -> listing.setUnparsedAddress(value));
        setFieldFromCsv(row, columnMapping, "city", value -> listing.setCity(value));
        setFieldFromCsv(row, columnMapping, "state", value -> listing.setStateOrProvince(value));
        setFieldFromCsv(row, columnMapping, "postalCode", value -> listing.setPostalCode(value));
        setFieldFromCsv(row, columnMapping, "description", value -> listing.setPublicRemarks(value));
        setFieldFromCsv(row, columnMapping, "status", value -> {
            try {
                listing.setStandardStatus(mapCsvStatus(value));
            } catch (Exception e) {
                log.warn("Invalid status value: {}", value);
            }
        });

        // Map numeric fields
        setNumericFieldFromCsv(row, columnMapping, "listPrice", value -> {
            try {
                listing.setListPrice(java.math.BigDecimal.valueOf(Double.parseDouble(value)));
            } catch (NumberFormatException e) {
                log.warn("Invalid list price: {}", value);
            }
        });

        setNumericFieldFromCsv(row, columnMapping, "bedrooms", value -> {
            try {
                listing.setBedroomsTotal(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid bedrooms count: {}", value);
            }
        });

        setNumericFieldFromCsv(row, columnMapping, "bathrooms", value -> {
            try {
                listing.setBathroomsTotalInteger(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid bathrooms count: {}", value);
            }
        });

        setNumericFieldFromCsv(row, columnMapping, "livingArea", value -> {
            try {
                listing.setLivingArea(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid living area: {}", value);
            }
        });

        setNumericFieldFromCsv(row, columnMapping, "latitude", value -> {
            try {
                listing.setLatitude(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid latitude: {}", value);
            }
        });

        setNumericFieldFromCsv(row, columnMapping, "longitude", value -> {
            try {
                listing.setLongitude(Double.parseDouble(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid longitude: {}", value);
            }
        });

        setNumericFieldFromCsv(row, columnMapping, "yearBuilt", value -> {
            try {
                listing.setYearBuilt(Integer.parseInt(value));
            } catch (NumberFormatException e) {
                log.warn("Invalid year built: {}", value);
            }
        });

        // Set timestamps
        LocalDateTime now = LocalDateTime.now();
        if (listing.getCreatedAt() == null) {
            listing.setCreatedAt(now);
        }
        listing.setUpdatedAt(now);
    }

    /**
     * Helper method to set string field from CSV
     */
    private void setFieldFromCsv(String[] row, Map<String, Integer> columnMapping, String fieldName, 
                                  java.util.function.Consumer<String> setter) {
        Integer columnIndex = columnMapping.get(fieldName);
        if (columnIndex != null && columnIndex < row.length) {
            String value = row[columnIndex];
            if (value != null && !value.trim().isEmpty()) {
                setter.accept(value.trim());
            }
        }
    }

    /**
     * Helper method to set numeric field from CSV
     */
    private void setNumericFieldFromCsv(String[] row, Map<String, Integer> columnMapping, String fieldName, 
                                        java.util.function.Consumer<String> setter) {
        Integer columnIndex = columnMapping.get(fieldName);
        if (columnIndex != null && columnIndex < row.length) {
            String value = row[columnIndex];
            if (value != null && !value.trim().isEmpty() && !value.equalsIgnoreCase("null")) {
                setter.accept(value.trim());
            }
        }
    }

    /**
     * Map CSV status to StandardStatus enum
     */
    private com.proptech.realestate.model.enums.StandardStatus mapCsvStatus(String csvStatus) {
        if (csvStatus == null) return com.proptech.realestate.model.enums.StandardStatus.ACTIVE;
        
        String status = csvStatus.toLowerCase().trim();
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
     * Update existing listing with new CSV data
     */
    private void updateExistingListing(Listing existing, Listing updated) {
        // Update key fields from CSV data
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

    // Helper methods for connection testing
    private boolean testHttpCsvAccess(String url) {
        try {
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean testLocalCsvAccess(String filePath) {
        try {
            File file = new File(filePath);
            return file.exists() && file.canRead();
        } catch (Exception e) {
            return false;
        }
    }

    private long getFileSize(String path) {
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
