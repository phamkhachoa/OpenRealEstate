package com.proptech.realestate.model.entity;

/**
 * Synchronization status for IDX feeds
 * Phase 7 Implementation
 */
public enum SyncStatus {
    /**
     * Synchronization completed successfully
     */
    SUCCESS("Success"),
    
    /**
     * Synchronization completed with some issues
     */
    PARTIAL_SUCCESS("Partial Success"),
    
    /**
     * Synchronization failed
     */
    FAILED("Failed"),
    
    /**
     * Synchronization is currently in progress
     */
    IN_PROGRESS("In Progress"),
    
    /**
     * Synchronization is scheduled but not started
     */
    SCHEDULED("Scheduled"),
    
    /**
     * Synchronization was cancelled
     */
    CANCELLED("Cancelled"),
    
    /**
     * Synchronization timed out
     */
    TIMEOUT("Timeout");

    private final String displayName;

    SyncStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Check if this status indicates a completed sync
     */
    public boolean isCompleted() {
        return this == SUCCESS || this == PARTIAL_SUCCESS || this == FAILED || 
               this == CANCELLED || this == TIMEOUT;
    }

    /**
     * Check if this status indicates a successful sync
     */
    public boolean isSuccessful() {
        return this == SUCCESS || this == PARTIAL_SUCCESS;
    }

    /**
     * Check if this status indicates sync is running
     */
    public boolean isRunning() {
        return this == IN_PROGRESS || this == SCHEDULED;
    }

    /**
     * Check if this status indicates an error
     */
    public boolean isError() {
        return this == FAILED || this == TIMEOUT;
    }
}
