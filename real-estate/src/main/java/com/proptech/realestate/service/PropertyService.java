package com.proptech.realestate.service;

import com.proptech.realestate.model.entity.Listing;
import com.proptech.realestate.model.entity.IdxFeed;
import com.proptech.realestate.model.dto.reso.PropertyResource;
import com.proptech.realestate.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing Property/Listing entities
 * Handles CRUD operations and business logic for properties
 * Phase 7 Implementation
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PropertyService {

    private final ListingRepository listingRepository;
    private final ResoMappingService resoMappingService;

    /**
     * Save a new property/listing
     */
    public Listing save(Listing listing) {
        if (listing.getId() == null) {
            listing.setCreatedAt(LocalDateTime.now());
        }
        listing.setUpdatedAt(LocalDateTime.now());
        
        log.debug("Saving listing: {}", listing.getListingId());
        return listingRepository.save(listing);
    }

    /**
     * Find property by listing key (used in IDX processing)
     */
    @Transactional(readOnly = true)
    public Listing findByListingKey(String listingKey) {
        try {
            UUID listingId = UUID.fromString(listingKey);
            return listingRepository.findById(listingId).orElse(null);
        } catch (IllegalArgumentException e) {
            // If listingKey is not a UUID, try finding by listingId field
            return listingRepository.findByListingId(listingKey).orElse(null);
        }
    }

    /**
     * Find property by MLS listing ID
     */
    @Transactional(readOnly = true)
    public Optional<Listing> findByListingId(String listingId) {
        return listingRepository.findByListingId(listingId);
    }

    /**
     * Find property by ID
     */
    @Transactional(readOnly = true)
    public Optional<Listing> findById(UUID id) {
        return listingRepository.findById(id);
    }

    /**
     * Find all active properties
     */
    @Transactional(readOnly = true)
    public List<Listing> findAllActive() {
        return listingRepository.findByStandardStatus("ACTIVE");
    }

    /**
     * Find properties by MLS ID
     */
    @Transactional(readOnly = true)
    public List<Listing> findByMlsId(String mlsId) {
        return listingRepository.findByMlsId(mlsId);
    }

    /**
     * Find properties modified after a certain timestamp
     */
    @Transactional(readOnly = true)
    public List<Listing> findModifiedAfter(LocalDateTime timestamp) {
        return listingRepository.findByUpdatedAtAfter(timestamp);
    }

    /**
     * Find properties modified after timestamp with pagination
     */
    @Transactional(readOnly = true)
    public Page<Listing> findModifiedAfter(LocalDateTime timestamp, Pageable pageable) {
        return listingRepository.findByUpdatedAtAfter(timestamp, pageable);
    }

    /**
     * Delete property by ID
     */
    public void deleteById(UUID id) {
        log.debug("Deleting listing: {}", id);
        listingRepository.deleteById(id);
    }

    /**
     * Soft delete property (mark as withdrawn)
     */
    public void softDelete(UUID id) {
        Optional<Listing> listingOpt = listingRepository.findById(id);
        if (listingOpt.isPresent()) {
            Listing listing = listingOpt.get();
            listing.setStandardStatus(com.proptech.realestate.model.enums.StandardStatus.WITHDRAWN);
            listing.setUpdatedAt(LocalDateTime.now());
            listingRepository.save(listing);
            log.debug("Soft deleted listing: {}", id);
        }
    }

    /**
     * Count total properties
     */
    @Transactional(readOnly = true)
    public long count() {
        return listingRepository.count();
    }

    /**
     * Count active properties
     */
    @Transactional(readOnly = true)
    public long countActive() {
        return listingRepository.countByStandardStatus("ACTIVE");
    }

    /**
     * Count properties by MLS
     */
    @Transactional(readOnly = true)
    public long countByMlsId(String mlsId) {
        return listingRepository.countByMlsId(mlsId);
    }

    /**
     * Check if property exists by listing key
     */
    @Transactional(readOnly = true)
    public boolean existsByListingKey(String listingKey) {
        return findByListingKey(listingKey) != null;
    }

    /**
     * Batch save properties (for IDX processing)
     */
    public List<Listing> saveAll(List<Listing> listings) {
        LocalDateTime now = LocalDateTime.now();
        
        for (Listing listing : listings) {
            if (listing.getId() == null) {
                listing.setCreatedAt(now);
            }
            listing.setUpdatedAt(now);
        }
        
        log.debug("Batch saving {} listings", listings.size());
        return listingRepository.saveAll(listings);
    }

    /**
     * Create property from RESO PropertyResource
     */
    public Listing createFromResoProperty(PropertyResource resoProperty, String mlsId) {
        log.debug("Creating listing from RESO property: {}", resoProperty.getListingKey());
        return resoMappingService.mapResoToProperty(resoProperty, mlsId);
    }

    /**
     * Update existing property with RESO data
     */
    public Listing updateWithResoProperty(Listing existing, PropertyResource resoProperty) {
        log.debug("Updating listing with RESO property: {}", resoProperty.getListingKey());
        
        // Only update if RESO data is newer
        if (resoProperty.getModificationTimestamp() != null &&
            (existing.getUpdatedAt() == null || 
             resoProperty.getModificationTimestamp().isAfter(existing.getUpdatedAt()))) {
            
            // Update key fields from RESO data
            if (resoProperty.getListPrice() != null) {
                existing.setListPrice(resoProperty.getListPrice());
            }
            if (resoProperty.getStandardStatus() != null) {
                existing.setStandardStatus(resoProperty.getStandardStatus());
            }
            if (resoProperty.getPublicRemarks() != null) {
                existing.setDescription(resoProperty.getPublicRemarks());
            }
            if (resoProperty.getBedroomsTotal() != null) {
                existing.setBedroomsTotal(resoProperty.getBedroomsTotal());
            }
            if (resoProperty.getBathroomsTotalInteger() != null) {
                existing.setBathroomsTotalInteger(resoProperty.getBathroomsTotalInteger());
            }
            if (resoProperty.getLivingArea() != null) {
                existing.setLivingArea(resoProperty.getLivingArea().doubleValue());
            }
            
            existing.setUpdatedAt(LocalDateTime.now());
        }
        
        return existing;
    }

    /**
     * Find properties for specific feed within date range
     */
    @Transactional(readOnly = true)
    public List<Listing> findForFeedSync(IdxFeed feed, LocalDateTime since) {
        if (since != null) {
            return listingRepository.findByMlsIdAndUpdatedAtAfter(feed.getMlsId(), since);
        } else {
            return listingRepository.findByMlsId(feed.getMlsId());
        }
    }

    /**
     * Get property statistics for MLS
     */
    @Transactional(readOnly = true)
    public PropertyStatistics getStatisticsForMls(String mlsId) {
        long total = countByMlsId(mlsId);
        long active = listingRepository.countByMlsIdAndStandardStatus(mlsId, "ACTIVE");
        long pending = listingRepository.countByMlsIdAndStandardStatus(mlsId, "PENDING");
        long closed = listingRepository.countByMlsIdAndStandardStatus(mlsId, "CLOSED");
        
        return PropertyStatistics.builder()
                .mlsId(mlsId)
                .totalProperties(total)
                .activeProperties(active)
                .pendingProperties(pending)
                .closedProperties(closed)
                .build();
    }

    /**
     * Property statistics DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class PropertyStatistics {
        private String mlsId;
        private long totalProperties;
        private long activeProperties;
        private long pendingProperties;
        private long closedProperties;
        
        public double getActivePercentage() {
            return totalProperties > 0 ? (double) activeProperties / totalProperties * 100 : 0.0;
        }
    }
}
