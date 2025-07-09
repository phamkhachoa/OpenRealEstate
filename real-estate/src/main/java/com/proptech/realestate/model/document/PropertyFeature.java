package com.proptech.realestate.model.document;

import lombok.*;

/**
 * Property feature for nested search
 * Phase 8 Implementation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class PropertyFeature {

    private String name;
    private String value;
    private String category; // "INTERIOR", "EXTERIOR", "COMMUNITY", "FINANCIAL", "LOCATION"
    private String type;     // "BOOLEAN", "STRING", "NUMBER", "LIST"
    private Integer sortOrder;

    /**
     * Feature categories
     */
    public enum Category {
        INTERIOR("Interior"),
        EXTERIOR("Exterior"),
        COMMUNITY("Community"),
        FINANCIAL("Financial"),
        LOCATION("Location"),
        UTILITIES("Utilities"),
        PARKING("Parking"),
        APPLIANCES("Appliances");

        private final String displayName;

        Category(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Feature types
     */
    public enum Type {
        BOOLEAN("Boolean"),
        STRING("String"),
        NUMBER("Number"),
        LIST("List"),
        DATE("Date");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Check if feature has a boolean value
     */
    public boolean isBooleanFeature() {
        return Type.BOOLEAN.name().equals(type);
    }

    /**
     * Check if feature has a numeric value
     */
    public boolean isNumericFeature() {
        return Type.NUMBER.name().equals(type);
    }

    /**
     * Get boolean value
     */
    public Boolean getBooleanValue() {
        if (isBooleanFeature() && value != null) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    /**
     * Get numeric value
     */
    public Double getNumericValue() {
        if (isNumericFeature() && value != null) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
