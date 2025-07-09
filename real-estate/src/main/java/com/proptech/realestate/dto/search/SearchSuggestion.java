package com.proptech.realestate.dto.search;

import lombok.*;

/**
 * Search suggestion DTO for auto-complete
 * Phase 8 Implementation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class SearchSuggestion {

    private String text;
    private String type; // "CITY", "ADDRESS", "NEIGHBORHOOD", "SCHOOL", "AGENT", "OFFICE"
    private String category;
    private Float score;
    private Integer frequency; // How often this suggestion appears
    private String description;
    private Object metadata;

    /**
     * Suggestion types
     */
    public enum Type {
        CITY("City"),
        ADDRESS("Address"),
        NEIGHBORHOOD("Neighborhood"),
        SCHOOL_DISTRICT("School District"),
        AGENT("Agent"),
        OFFICE("Office"),
        PROPERTY_TYPE("Property Type"),
        FEATURE("Feature"),
        ZIPCODE("ZIP Code");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Get formatted suggestion text
     */
    public String getFormattedText() {
        if (description != null && !description.isEmpty()) {
            return text + " (" + description + ")";
        }
        return text;
    }

    /**
     * Check if this is a location suggestion
     */
    public boolean isLocationSuggestion() {
        return "CITY".equals(type) || "ADDRESS".equals(type) || 
               "NEIGHBORHOOD".equals(type) || "ZIPCODE".equals(type);
    }

    /**
     * Check if this is a high-frequency suggestion
     */
    public boolean isPopularSuggestion() {
        return frequency != null && frequency > 100;
    }

    /**
     * Get relevance score as percentage
     */
    public Double getRelevancePercentage() {
        if (score == null) {
            return null;
        }
        return Math.min(100.0, score * 100.0);
    }
}
