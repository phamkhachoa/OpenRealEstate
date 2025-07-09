package com.proptech.realestate.service.idx;

import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.service.PropertyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for aggregating and deduplicating IDX data from multiple MLS sources
 * Handles data quality, standardization, and conflict resolution
 * Phase 7 Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IdxDataAggregationService {

    private final PropertyService propertyService;

    /**
     * Process new properties from IDX feed - handle deduplication and aggregation
     */
    public AggregationResult processNewProperties(List<Listing> newProperties, String sourceMlsId) {
        if (newProperties == null || newProperties.isEmpty()) {
            return AggregationResult.empty();
        }

        log.info("Processing {} new properties from MLS: {}", newProperties.size(), sourceMlsId);

        AggregationResult result = AggregationResult.builder()
                .sourceMlsId(sourceMlsId)
                .totalProcessed(newProperties.size())
                .processedAt(LocalDateTime.now())
                .build();

        List<Listing> duplicatesFound = new ArrayList<>();
        List<Listing> uniqueProperties = new ArrayList<>();
        List<PropertyConflict> conflicts = new ArrayList<>();

        for (Listing property : newProperties) {
            try {
                // Check for potential duplicates
                List<Listing> potentialDuplicates = findPotentialDuplicates(property);

                if (potentialDuplicates.isEmpty()) {
                    // No duplicates found, it's a unique property
                    uniqueProperties.add(property);
                    result.incrementUnique();
                } else {
                    // Found potential duplicates, resolve conflicts
                    PropertyConflict conflict = resolvePropertyConflicts(property, potentialDuplicates);
                    conflicts.add(conflict);
                    
                    if (conflict.getResolution() == ConflictResolution.MERGE) {
                        duplicatesFound.add(property);
                        result.incrementMerged();
                    } else if (conflict.getResolution() == ConflictResolution.UPDATE) {
                        uniqueProperties.add(property);
                        result.incrementUpdated();
                    } else {
                        result.incrementSkipped();
                    }
                }

            } catch (Exception e) {
                log.error("Failed to process property: {}", property.getListingId(), e);
                result.incrementErrors();
                result.addError("Property " + property.getListingId() + ": " + e.getMessage());
            }
        }

        result.setUniqueProperties(uniqueProperties);
        result.setDuplicates(duplicatesFound);
        result.setConflicts(conflicts);

        // Calculate quality score
        double qualityScore = calculateDataQualityScore(newProperties);
        result.setDataQualityScore(qualityScore);

        log.info("Aggregation completed for MLS {}: {} unique, {} duplicates, {} conflicts, quality score: {:.2f}",
                sourceMlsId, result.getUniqueCount(), result.getMergedCount(), conflicts.size(), qualityScore);

        return result;
    }

    /**
     * Find potential duplicate properties using multiple matching strategies
     */
    private List<Listing> findPotentialDuplicates(Listing property) {
        List<Listing> duplicates = new ArrayList<>();

        // Strategy 1: Exact address match within same MLS
        if (property.getUnparsedAddress() != null && property.getMlsId() != null) {
            List<Listing> addressMatches = findByAddressAndMls(property.getUnparsedAddress(), property.getMlsId());
            duplicates.addAll(addressMatches);
        }

        // Strategy 2: Geographic proximity + similar characteristics
        if (property.getLatitude() != null && property.getLongitude() != null) {
            List<Listing> proximityMatches = findByProximityAndCharacteristics(property);
            duplicates.addAll(proximityMatches);
        }

        // Strategy 3: Similar listing ID pattern (for cross-MLS matching)
        if (property.getListingId() != null) {
            List<Listing> idMatches = findBySimilarListingId(property.getListingId());
            duplicates.addAll(idMatches);
        }

        // Remove exact same property and deduplicate results
        return duplicates.stream()
                .filter(p -> !p.getId().equals(property.getId()))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Resolve conflicts between properties
     */
    private PropertyConflict resolvePropertyConflicts(Listing newProperty, List<Listing> existingProperties) {
        PropertyConflict conflict = PropertyConflict.builder()
                .newProperty(newProperty)
                .existingProperties(existingProperties)
                .detectedAt(LocalDateTime.now())
                .build();

        // Find the best matching existing property
        Listing bestMatch = findBestMatch(newProperty, existingProperties);
        
        if (bestMatch != null) {
            conflict.setBestMatch(bestMatch);
            
            // Compare modification timestamps
            if (newProperty.getModificationTimestamp() != null && 
                bestMatch.getModificationTimestamp() != null) {
                
                if (newProperty.getModificationTimestamp().isAfter(bestMatch.getModificationTimestamp())) {
                    // New property has newer data, update existing
                    conflict.setResolution(ConflictResolution.UPDATE);
                    conflict.setResolvedProperty(mergeProperties(bestMatch, newProperty));
                } else {
                    // Existing property is newer or same, skip new one
                    conflict.setResolution(ConflictResolution.SKIP);
                    conflict.setResolvedProperty(bestMatch);
                }
            } else {
                // No timestamp comparison possible, merge properties
                conflict.setResolution(ConflictResolution.MERGE);
                conflict.setResolvedProperty(mergeProperties(bestMatch, newProperty));
            }
        } else {
            // No good match found, treat as unique
            conflict.setResolution(ConflictResolution.CREATE_NEW);
            conflict.setResolvedProperty(newProperty);
        }

        return conflict;
    }

    /**
     * Find the best match among existing properties
     */
    private Listing findBestMatch(Listing newProperty, List<Listing> existingProperties) {
        double bestScore = 0.0;
        Listing bestMatch = null;

        for (Listing existing : existingProperties) {
            double score = calculateSimilarityScore(newProperty, existing);
            if (score > bestScore && score > 0.8) { // 80% similarity threshold
                bestScore = score;
                bestMatch = existing;
            }
        }

        return bestMatch;
    }

    /**
     * Calculate similarity score between two properties
     */
    private double calculateSimilarityScore(Listing property1, Listing property2) {
        double score = 0.0;
        int factors = 0;

        // Address similarity (high weight)
        if (property1.getUnparsedAddress() != null && property2.getUnparsedAddress() != null) {
            double addressSimilarity = calculateStringSimilarity(
                property1.getUnparsedAddress().toLowerCase(),
                property2.getUnparsedAddress().toLowerCase()
            );
            score += addressSimilarity * 0.4; // 40% weight
            factors++;
        }

        // Geographic proximity (high weight)
        if (property1.getLatitude() != null && property1.getLongitude() != null &&
            property2.getLatitude() != null && property2.getLongitude() != null) {
            double distance = calculateDistance(
                property1.getLatitude(), property1.getLongitude(),
                property2.getLatitude(), property2.getLongitude()
            );
            double proximityScore = distance < 0.1 ? 1.0 : Math.max(0, 1.0 - distance / 0.5); // Within 0.5km
            score += proximityScore * 0.3; // 30% weight
            factors++;
        }

        // Price similarity (medium weight)
        if (property1.getListPrice() != null && property2.getListPrice() != null) {
            double priceDiff = Math.abs(property1.getListPrice().subtract(property2.getListPrice()).doubleValue());
            double avgPrice = property1.getListPrice().add(property2.getListPrice()).doubleValue() / 2;
            double priceScore = avgPrice > 0 ? Math.max(0, 1.0 - priceDiff / avgPrice) : 1.0;
            score += priceScore * 0.2; // 20% weight
            factors++;
        }

        // Property characteristics similarity (low weight)
        double charScore = 0.0;
        int charFactors = 0;

        if (Objects.equals(property1.getBedroomsTotal(), property2.getBedroomsTotal())) {
            charScore += 1.0;
        }
        charFactors++;

        if (Objects.equals(property1.getBathroomsTotalInteger(), property2.getBathroomsTotalInteger())) {
            charScore += 1.0;
        }
        charFactors++;

        if (Objects.equals(property1.getPropertyType(), property2.getPropertyType())) {
            charScore += 1.0;
        }
        charFactors++;

        if (charFactors > 0) {
            score += (charScore / charFactors) * 0.1; // 10% weight
            factors++;
        }

        return factors > 0 ? score / factors : 0.0;
    }

    /**
     * Merge two properties, keeping the best data from each
     */
    private Listing mergeProperties(Listing existing, Listing newProperty) {
        // Create a copy of existing property
        Listing merged = new Listing();
        merged.setId(existing.getId()); // Keep existing ID
        
        // Use newer modification timestamp
        merged.setModificationTimestamp(
            newProperty.getModificationTimestamp() != null ? 
            newProperty.getModificationTimestamp() : existing.getModificationTimestamp()
        );

        // Merge price information (prefer newer/non-null values)
        merged.setListPrice(newProperty.getListPrice() != null ? newProperty.getListPrice() : existing.getListPrice());
        merged.setOriginalListPrice(newProperty.getOriginalListPrice() != null ? 
            newProperty.getOriginalListPrice() : existing.getOriginalListPrice());

        // Merge property details
        merged.setBedroomsTotal(newProperty.getBedroomsTotal() != null ? 
            newProperty.getBedroomsTotal() : existing.getBedroomsTotal());
        merged.setBathroomsTotalInteger(newProperty.getBathroomsTotalInteger() != null ? 
            newProperty.getBathroomsTotalInteger() : existing.getBathroomsTotalInteger());
        merged.setLivingArea(newProperty.getLivingArea() != null ? 
            newProperty.getLivingArea() : existing.getLivingArea());

        // Merge status (prefer newer)
        merged.setStandardStatus(newProperty.getStandardStatus() != null ? 
            newProperty.getStandardStatus() : existing.getStandardStatus());

        // Merge descriptions (prefer longer, more detailed)
        String description = selectBetterDescription(existing.getPublicRemarks(), newProperty.getPublicRemarks());
        merged.setPublicRemarks(description);

        // Keep better location data
        if (newProperty.getLatitude() != null && newProperty.getLongitude() != null) {
            merged.setLatitude(newProperty.getLatitude());
            merged.setLongitude(newProperty.getLongitude());
        } else {
            merged.setLatitude(existing.getLatitude());
            merged.setLongitude(existing.getLongitude());
        }

        // Set update timestamp
        merged.setUpdatedAt(LocalDateTime.now());

        return merged;
    }

    /**
     * Calculate data quality score for a batch of properties
     */
    private double calculateDataQualityScore(List<Listing> properties) {
        if (properties.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;
        for (Listing property : properties) {
            totalScore += calculatePropertyQualityScore(property);
        }

        return totalScore / properties.size();
    }

    /**
     * Calculate quality score for individual property
     */
    private double calculatePropertyQualityScore(Listing property) {
        double score = 0.0;
        int maxPoints = 0;

        // Required fields (high impact)
        if (property.getListingId() != null && !property.getListingId().trim().isEmpty()) score += 10;
        maxPoints += 10;
        
        if (property.getListPrice() != null && property.getListPrice().compareTo(BigDecimal.ZERO) > 0) score += 10;
        maxPoints += 10;
        
        if (property.getUnparsedAddress() != null && !property.getUnparsedAddress().trim().isEmpty()) score += 10;
        maxPoints += 10;

        // Important fields (medium impact)
        if (property.getBedroomsTotal() != null && property.getBedroomsTotal() > 0) score += 5;
        maxPoints += 5;
        
        if (property.getBathroomsTotalInteger() != null && property.getBathroomsTotalInteger() > 0) score += 5;
        maxPoints += 5;
        
        if (property.getLivingArea() != null && property.getLivingArea() > 0) score += 5;
        maxPoints += 5;

        // Geographic data (medium impact)
        if (property.getLatitude() != null && property.getLongitude() != null) score += 8;
        maxPoints += 8;

        // Descriptive data (low impact)
        if (property.getPublicRemarks() != null && property.getPublicRemarks().length() > 50) score += 3;
        maxPoints += 3;
        
        if (property.getPropertyType() != null && !property.getPropertyType().trim().isEmpty()) score += 2;
        maxPoints += 2;

        return maxPoints > 0 ? (score / maxPoints) * 100.0 : 0.0;
    }

    // Helper methods for duplicate detection
    private List<Listing> findByAddressAndMls(String address, String mlsId) {
        // This would use PropertyService to find properties
        // Implementation depends on available repository methods
        return new ArrayList<>(); // Placeholder
    }

    private List<Listing> findByProximityAndCharacteristics(Listing property) {
        // This would use geographic search to find nearby properties
        return new ArrayList<>(); // Placeholder
    }

    private List<Listing> findBySimilarListingId(String listingId) {
        // This would use pattern matching to find similar listing IDs
        return new ArrayList<>(); // Placeholder
    }

    private double calculateStringSimilarity(String str1, String str2) {
        // Simple Levenshtein distance implementation
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];
        
        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(
                        dp[i - 1][j - 1] + (str1.charAt(i - 1) == str2.charAt(j - 1) ? 0 : 1),
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1)
                    );
                }
            }
        }
        
        int maxLen = Math.max(str1.length(), str2.length());
        return maxLen > 0 ? 1.0 - (double) dp[str1.length()][str2.length()] / maxLen : 1.0;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Haversine formula for distance calculation
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }

    private String selectBetterDescription(String desc1, String desc2) {
        if (desc1 == null && desc2 == null) return null;
        if (desc1 == null) return desc2;
        if (desc2 == null) return desc1;
        
        // Prefer longer description
        return desc1.length() >= desc2.length() ? desc1 : desc2;
    }

    // DTOs for aggregation results
    @lombok.Data
    @lombok.Builder
    public static class AggregationResult {
        private String sourceMlsId;
        private int totalProcessed;
        private int uniqueCount;
        private int mergedCount;
        private int updatedCount;
        private int skippedCount;
        private int errorCount;
        private double dataQualityScore;
        private LocalDateTime processedAt;
        
        private List<Listing> uniqueProperties;
        private List<Listing> duplicates;
        private List<PropertyConflict> conflicts;
        private List<String> errors;

        public static AggregationResult empty() {
            return AggregationResult.builder()
                    .totalProcessed(0)
                    .uniqueCount(0)
                    .mergedCount(0)
                    .updatedCount(0)
                    .skippedCount(0)
                    .errorCount(0)
                    .dataQualityScore(0.0)
                    .processedAt(LocalDateTime.now())
                    .uniqueProperties(new ArrayList<>())
                    .duplicates(new ArrayList<>())
                    .conflicts(new ArrayList<>())
                    .errors(new ArrayList<>())
                    .build();
        }

        public void incrementUnique() { uniqueCount++; }
        public void incrementMerged() { mergedCount++; }
        public void incrementUpdated() { updatedCount++; }
        public void incrementSkipped() { skippedCount++; }
        public void incrementErrors() { errorCount++; }
        
        public void addError(String error) {
            if (errors == null) errors = new ArrayList<>();
            errors.add(error);
        }
    }

    @lombok.Data
    @lombok.Builder
    public static class PropertyConflict {
        private Listing newProperty;
        private List<Listing> existingProperties;
        private Listing bestMatch;
        private Listing resolvedProperty;
        private ConflictResolution resolution;
        private LocalDateTime detectedAt;
        private String notes;
    }

    public enum ConflictResolution {
        CREATE_NEW,     // No conflict, create new property
        MERGE,          // Merge data from multiple sources
        UPDATE,         // Update existing with newer data
        SKIP            // Skip new property, keep existing
    }
}
