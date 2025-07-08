package com.proptech.realestate.service;

import com.proptech.realestate.model.entity.MLSRegion;
import com.proptech.realestate.repository.MLSRegionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing MLS regions.
 * Provides business logic for MLS region operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MLSRegionService {

    private final MLSRegionRepository mlsRegionRepository;

    /**
     * Create a new MLS region.
     */
    public MLSRegion createRegion(MLSRegion region) {
        log.info("Creating new MLS region: {}", region.getName());
        
        // Validate unique region code
        if (mlsRegionRepository.existsByRegionCode(region.getRegionCode())) {
            throw new IllegalArgumentException("Region code already exists: " + region.getRegionCode());
        }
        
        // Set default values
        if (region.getIsActive() == null) {
            region.setIsActive(true);
        }
        
        if (region.getMaxListingDays() == null) {
            region.setMaxListingDays(180);
        }
        
        if (region.getRequiresApproval() == null) {
            region.setRequiresApproval(false);
        }
        
        if (region.getAllowsPrivateRemarks() == null) {
            region.setAllowsPrivateRemarks(true);
        }
        
        if (region.getMandatoryShowingTimeHours() == null) {
            region.setMandatoryShowingTimeHours(24);
        }
        
        MLSRegion savedRegion = mlsRegionRepository.save(region);
        log.info("Created MLS region with ID: {}", savedRegion.getId());
        
        return savedRegion;
    }

    /**
     * Update an existing MLS region.
     */
    public MLSRegion updateRegion(UUID regionId, MLSRegion updatedRegion) {
        log.info("Updating MLS region: {}", regionId);
        
        MLSRegion existingRegion = mlsRegionRepository.findById(regionId)
            .orElseThrow(() -> new EntityNotFoundException("MLS region not found: " + regionId));
        
        // Validate unique region code if changed
        if (!existingRegion.getRegionCode().equals(updatedRegion.getRegionCode()) &&
            mlsRegionRepository.existsByRegionCodeAndIdNot(updatedRegion.getRegionCode(), regionId)) {
            throw new IllegalArgumentException("Region code already exists: " + updatedRegion.getRegionCode());
        }
        
        // Update fields
        existingRegion.setRegionCode(updatedRegion.getRegionCode());
        existingRegion.setName(updatedRegion.getName());
        existingRegion.setDescription(updatedRegion.getDescription());
        existingRegion.setIsActive(updatedRegion.getIsActive());
        existingRegion.setNorthBoundary(updatedRegion.getNorthBoundary());
        existingRegion.setSouthBoundary(updatedRegion.getSouthBoundary());
        existingRegion.setEastBoundary(updatedRegion.getEastBoundary());
        existingRegion.setWestBoundary(updatedRegion.getWestBoundary());
        existingRegion.setListingPrefix(updatedRegion.getListingPrefix());
        existingRegion.setMaxListingDays(updatedRegion.getMaxListingDays());
        existingRegion.setRequiresApproval(updatedRegion.getRequiresApproval());
        existingRegion.setAllowsPrivateRemarks(updatedRegion.getAllowsPrivateRemarks());
        existingRegion.setMandatoryShowingTimeHours(updatedRegion.getMandatoryShowingTimeHours());
        existingRegion.setAdminEmail(updatedRegion.getAdminEmail());
        existingRegion.setAdminPhone(updatedRegion.getAdminPhone());
        existingRegion.setWebsiteUrl(updatedRegion.getWebsiteUrl());
        
        MLSRegion savedRegion = mlsRegionRepository.save(existingRegion);
        log.info("Updated MLS region: {}", savedRegion.getId());
        
        return savedRegion;
    }

    /**
     * Get MLS region by ID.
     */
    @Transactional(readOnly = true)
    public MLSRegion getRegionById(UUID regionId) {
        return mlsRegionRepository.findById(regionId)
            .orElseThrow(() -> new EntityNotFoundException("MLS region not found: " + regionId));
    }

    /**
     * Get MLS region by region code.
     */
    @Transactional(readOnly = true)
    public Optional<MLSRegion> getRegionByCode(String regionCode) {
        return mlsRegionRepository.findByRegionCode(regionCode);
    }

    /**
     * Get all active MLS regions.
     */
    @Transactional(readOnly = true)
    public List<MLSRegion> getActiveRegions() {
        return mlsRegionRepository.findByIsActiveTrue();
    }

    /**
     * Get all MLS regions.
     */
    @Transactional(readOnly = true)
    public List<MLSRegion> getAllRegions() {
        return mlsRegionRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Search regions by criteria.
     */
    @Transactional(readOnly = true)
    public List<MLSRegion> searchRegions(String name, String regionCode, Boolean isActive) {
        return mlsRegionRepository.searchRegions(name, regionCode, isActive);
    }

    /**
     * Find region by location coordinates.
     */
    @Transactional(readOnly = true)
    public Optional<MLSRegion> findRegionByLocation(Double latitude, Double longitude) {
        return mlsRegionRepository.findFirstByLocationWithinBoundaries(latitude, longitude);
    }

    /**
     * Find all regions that contain the location.
     */
    @Transactional(readOnly = true)
    public List<MLSRegion> findRegionsByLocation(Double latitude, Double longitude) {
        return mlsRegionRepository.findByLocationWithinBoundaries(latitude, longitude);
    }

    /**
     * Activate a region.
     */
    public MLSRegion activateRegion(UUID regionId) {
        log.info("Activating MLS region: {}", regionId);
        
        MLSRegion region = getRegionById(regionId);
        region.setIsActive(true);
        
        return mlsRegionRepository.save(region);
    }

    /**
     * Deactivate a region.
     */
    public MLSRegion deactivateRegion(UUID regionId) {
        log.info("Deactivating MLS region: {}", regionId);
        
        MLSRegion region = getRegionById(regionId);
        region.setIsActive(false);
        
        return mlsRegionRepository.save(region);
    }

    /**
     * Delete a region (soft delete by deactivating).
     */
    public void deleteRegion(UUID regionId) {
        log.info("Deleting MLS region: {}", regionId);
        
        MLSRegion region = getRegionById(regionId);
        
        // Check if region has active memberships or listings
        // This would require additional repository methods
        // For now, just deactivate
        region.setIsActive(false);
        mlsRegionRepository.save(region);
    }

    /**
     * Generate MLS number for a listing in the region.
     */
    public String generateMLSNumber(UUID regionId) {
        MLSRegion region = getRegionById(regionId);
        
        // This is a simplified implementation
        // In production, you'd want a more sophisticated sequence management
        long sequence = System.currentTimeMillis() % 1000000;
        
        return region.generateMLSNumber(sequence);
    }

    /**
     * Check if region code is available.
     */
    @Transactional(readOnly = true)
    public boolean isRegionCodeAvailable(String regionCode) {
        return !mlsRegionRepository.existsByRegionCode(regionCode);
    }

    /**
     * Check if region code is available for update (excluding current ID).
     */
    @Transactional(readOnly = true)
    public boolean isRegionCodeAvailable(String regionCode, UUID excludeId) {
        return !mlsRegionRepository.existsByRegionCodeAndIdNot(regionCode, excludeId);
    }

    /**
     * Get region statistics.
     */
    @Transactional(readOnly = true)
    public RegionStats getRegionStats(UUID regionId) {
        MLSRegion region = getRegionById(regionId);
        
        // This would require additional repository methods to get actual counts
        // For now, return basic stats
        return RegionStats.builder()
            .regionId(regionId)
            .regionName(region.getName())
            .isActive(region.getIsActive())
            .totalMemberships(0L) // Would be calculated from repository
            .activeMemberships(0L) // Would be calculated from repository
            .totalListings(0L) // Would be calculated from repository
            .activeListings(0L) // Would be calculated from repository
            .totalOffices(0L) // Would be calculated from repository
            .build();
    }

    /**
     * Statistics data class for region information.
     */
    @lombok.Data
    @lombok.Builder
    public static class RegionStats {
        private UUID regionId;
        private String regionName;
        private Boolean isActive;
        private Long totalMemberships;
        private Long activeMemberships;
        private Long totalListings;
        private Long activeListings;
        private Long totalOffices;
    }
} 