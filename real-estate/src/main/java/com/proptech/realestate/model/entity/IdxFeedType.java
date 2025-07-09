package com.proptech.realestate.model.entity;

/**
 * IDX Feed Types supported by the system
 * Phase 7 Implementation
 */
public enum IdxFeedType {
    /**
     * RETS 1.7.2 - Legacy Real Estate Transaction Standard
     */
    RETS_1_7_2("RETS 1.7.2", "application/xml"),
    
    /**
     * RETS 1.8 - Updated Real Estate Transaction Standard
     */
    RETS_1_8("RETS 1.8", "application/xml"),
    
    /**
     * RESO Web API - Modern REST API standard
     */
    RESO_WEB_API("RESO Web API", "application/json"),
    
    /**
     * CSV Feed - Comma Separated Values file
     */
    CSV_FEED("CSV Feed", "text/csv"),
    
    /**
     * XML Feed - Custom XML format
     */
    XML_FEED("XML Feed", "application/xml"),
    
    /**
     * JSON Feed - Custom JSON format
     */
    JSON_FEED("JSON Feed", "application/json"),
    
    /**
     * FTP Feed - File Transfer Protocol based
     */
    FTP_FEED("FTP Feed", "application/octet-stream");

    private final String displayName;
    private final String contentType;

    IdxFeedType(String displayName, String contentType) {
        this.displayName = displayName;
        this.contentType = contentType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getContentType() {
        return contentType;
    }

    /**
     * Check if this feed type requires XML processing
     */
    public boolean isXmlBased() {
        return this == RETS_1_7_2 || this == RETS_1_8 || this == XML_FEED;
    }

    /**
     * Check if this feed type requires JSON processing
     */
    public boolean isJsonBased() {
        return this == RESO_WEB_API || this == JSON_FEED;
    }

    /**
     * Check if this feed type requires file processing
     */
    public boolean isFileBased() {
        return this == CSV_FEED || this == FTP_FEED;
    }

    /**
     * Check if this feed type supports real-time updates
     */
    public boolean supportsRealTimeUpdates() {
        return this == RESO_WEB_API;
    }
}
