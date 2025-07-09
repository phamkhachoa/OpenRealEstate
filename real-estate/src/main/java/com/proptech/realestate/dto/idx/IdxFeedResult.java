package com.proptech.realestate.dto.idx;

import com.proptech.realestate.model.entity.Property;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Result object for IDX feed processing
 * Phase 7 Implementation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class IdxFeedResult {

    @Builder.Default
    private List<Property> properties = new ArrayList<>();

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    private Integer totalProcessed = 0;
    private Integer totalCreated = 0;
    private Integer totalUpdated = 0;
    private Integer totalDeleted = 0;
    private Integer totalSkipped = 0;
    private Integer totalDuplicated = 0;

    private Long processingTimeMs;
    private Double recordsPerSecond;

    private Map<String, Object> statistics;
    private Map<String, Object> metadata;

    /**
     * Add a processed property
     */
    public void addProcessedProperty(Property property) {
        if (property != null) {
            properties.add(property);
            totalProcessed = properties.size();
        }
    }

    /**
     * Add an error message
     */
    public void addError(String error) {
        if (error != null && !error.trim().isEmpty()) {
            errors.add(error);
        }
    }

    /**
     * Add a warning message
     */
    public void addWarning(String warning) {
        if (warning != null && !warning.trim().isEmpty()) {
            warnings.add(warning);
        }
    }

    /**
     * Increment created count
     */
    public void incrementCreated() {
        totalCreated++;
    }

    /**
     * Increment updated count
     */
    public void incrementUpdated() {
        totalUpdated++;
    }

    /**
     * Increment deleted count
     */
    public void incrementDeleted() {
        totalDeleted++;
    }

    /**
     * Increment skipped count
     */
    public void incrementSkipped() {
        totalSkipped++;
    }

    /**
     * Increment duplicated count
     */
    public void incrementDuplicated() {
        totalDuplicated++;
    }

    /**
     * Check if processing was successful
     */
    public boolean isSuccessful() {
        return errors.isEmpty() || (getTotalAffected() > 0 && errors.size() < totalProcessed / 2);
    }

    /**
     * Check if processing had warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    /**
     * Get total properties affected (created + updated + deleted)
     */
    public Integer getTotalAffected() {
        return totalCreated + totalUpdated + totalDeleted;
    }

    /**
     * Get success rate as percentage
     */
    public Double getSuccessRate() {
        if (totalProcessed == 0) {
            return 0.0;
        }
        return (getTotalAffected() * 100.0) / totalProcessed;
    }

    /**
     * Get error rate as percentage
     */
    public Double getErrorRate() {
        if (totalProcessed == 0) {
            return 0.0;
        }
        return (errors.size() * 100.0) / totalProcessed;
    }

    /**
     * Calculate processing rate
     */
    public void calculateProcessingRate() {
        if (processingTimeMs != null && processingTimeMs > 0 && totalProcessed > 0) {
            recordsPerSecond = (totalProcessed * 1000.0) / processingTimeMs;
        }
    }

    /**
     * Get summary statistics
     */
    public String getSummary() {
        return String.format(
            "Processed: %d, Created: %d, Updated: %d, Deleted: %d, Skipped: %d, Errors: %d, Success Rate: %.1f%%",
            totalProcessed, totalCreated, totalUpdated, totalDeleted, totalSkipped, 
            errors.size(), getSuccessRate()
        );
    }

    /**
     * Merge results from another IDX feed result
     */
    public void merge(IdxFeedResult other) {
        if (other != null) {
            properties.addAll(other.getProperties());
            errors.addAll(other.getErrors());
            warnings.addAll(other.getWarnings());
            
            totalProcessed += other.getTotalProcessed();
            totalCreated += other.getTotalCreated();
            totalUpdated += other.getTotalUpdated();
            totalDeleted += other.getTotalDeleted();
            totalSkipped += other.getTotalSkipped();
            totalDuplicated += other.getTotalDuplicated();
        }
    }
}
