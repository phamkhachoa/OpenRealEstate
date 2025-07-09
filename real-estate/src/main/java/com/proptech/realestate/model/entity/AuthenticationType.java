package com.proptech.realestate.model.entity;

/**
 * Authentication types for IDX feeds
 * Phase 7 Implementation
 */
public enum AuthenticationType {
    /**
     * Basic HTTP Authentication
     */
    BASIC_AUTH("Basic Authentication"),
    
    /**
     * API Key in header
     */
    API_KEY("API Key"),
    
    /**
     * OAuth 2.0 Authentication
     */
    OAUTH2("OAuth 2.0"),
    
    /**
     * Digest Authentication
     */
    DIGEST_AUTH("Digest Authentication"),
    
    /**
     * Bearer Token
     */
    BEARER_TOKEN("Bearer Token"),
    
    /**
     * No authentication required
     */
    NONE("No Authentication");

    private final String displayName;

    AuthenticationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this auth type requires username/password
     */
    public boolean requiresCredentials() {
        return this == BASIC_AUTH || this == DIGEST_AUTH;
    }

    /**
     * Check if this auth type requires API key
     */
    public boolean requiresApiKey() {
        return this == API_KEY || this == BEARER_TOKEN;
    }

    /**
     * Check if this auth type requires OAuth configuration
     */
    public boolean requiresOAuth() {
        return this == OAUTH2;
    }
}
