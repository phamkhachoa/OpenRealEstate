package com.proptech.realestate.service.idx;

import com.proptech.realestate.dto.idx.IdxFeedResult;
import com.proptech.realestate.model.entity.IdxFeed;

/**
 * Interface for IDX Feed processors
 * Phase 7 Implementation
 */
public interface IdxFeedProcessor {

    /**
     * Process an IDX feed and return results
     */
    IdxFeedResult processFeed(IdxFeed feed);

    /**
     * Check if this processor supports the given feed type
     */
    boolean supports(String feedType);

    /**
     * Get processor name
     */
    String getProcessorName();

    /**
     * Validate feed configuration
     */
    boolean validateFeedConfiguration(IdxFeed feed);

    /**
     * Test feed connectivity
     */
    boolean testConnection(IdxFeed feed);

    /**
     * Get estimated processing time
     */
    Long getEstimatedProcessingTime(IdxFeed feed);
}
