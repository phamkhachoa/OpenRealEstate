package com.proptech.realestate.controller;

import com.proptech.realestate.model.entity.MLSRegion;
import com.proptech.realestate.service.MLSRegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for MLS region operations.
 * Provides endpoints for managing MLS regions.
 */
@RestController
@RequestMapping("/api/v1/mls/regions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class MLSRegionController {

    private final MLSRegionService mlsRegionService;

    /**
     * Create a new MLS region.
     * POST /api/v1/mls/regions
     */
    @PostMapping
    public ResponseEntity<MLSRegion> createRegion(@Valid @RequestBody CreateRegionRequest request) {
        log.info("Creating new MLS region: {}", request.getName());
        
        MLSRegion region = new MLSRegion();
        region.setRegionCode(request.getRegionCode());
        region.setName(request.getName());
        region.setDescription(request.getDescription());
        region.setNorthBoundary(request.getNorthBoundary());
        region.setSouthBoundary(request.getSouthBoundary());
        region.setEastBoundary(request.getEastBoundary());
        region.setWestBoundary(request.getWestBoundary());
        region.setListingPrefix(request.getListingPrefix());
        region.setMaxListingDays(request.getMaxListingDays());
        region.setRequiresApproval(request.getRequiresApproval());
        region.setAllowsPrivateRemarks(request.getAllowsPrivateRemarks());
        region.setMandatoryShowingTimeHours(request.getMandatoryShowingTimeHours());
        region.setAdminEmail(request.getAdminEmail());
        region.setAdminPhone(request.getAdminPhone());
        region.setWebsiteUrl(request.getWebsiteUrl());
        
        MLSRegion savedRegion = mlsRegionService.createRegion(region);
        
        return new ResponseEntity<>(savedRegion, HttpStatus.CREATED);
    }

    /**
     * Update an existing MLS region.
     * PUT /api/v1/mls/regions/{regionId}
     */
    @PutMapping("/{regionId}")
    public ResponseEntity<MLSRegion> updateRegion(
            @PathVariable UUID regionId,
            @Valid @RequestBody UpdateRegionRequest request) {
        log.info("Updating MLS region: {}", regionId);
        
        MLSRegion updatedRegion = new MLSRegion();
        updatedRegion.setRegionCode(request.getRegionCode());
        updatedRegion.setName(request.getName());
        updatedRegion.setDescription(request.getDescription());
        updatedRegion.setIsActive(request.getIsActive());
        updatedRegion.setNorthBoundary(request.getNorthBoundary());
        updatedRegion.setSouthBoundary(request.getSouthBoundary());
        updatedRegion.setEastBoundary(request.getEastBoundary());
        updatedRegion.setWestBoundary(request.getWestBoundary());
        updatedRegion.setListingPrefix(request.getListingPrefix());
        updatedRegion.setMaxListingDays(request.getMaxListingDays());
        updatedRegion.setRequiresApproval(request.getRequiresApproval());
        updatedRegion.setAllowsPrivateRemarks(request.getAllowsPrivateRemarks());
        updatedRegion.setMandatoryShowingTimeHours(request.getMandatoryShowingTimeHours());
        updatedRegion.setAdminEmail(request.getAdminEmail());
        updatedRegion.setAdminPhone(request.getAdminPhone());
        updatedRegion.setWebsiteUrl(request.getWebsiteUrl());
        
        MLSRegion savedRegion = mlsRegionService.updateRegion(regionId, updatedRegion);
        
        return ResponseEntity.ok(savedRegion);
    }

    /**
     * Get MLS region by ID.
     * GET /api/v1/mls/regions/{regionId}
     */
    @GetMapping("/{regionId}")
    public ResponseEntity<MLSRegion> getRegionById(@PathVariable UUID regionId) {
        log.info("Getting MLS region: {}", regionId);
        
        MLSRegion region = mlsRegionService.getRegionById(regionId);
        
        return ResponseEntity.ok(region);
    }

    /**
     * Get MLS region by region code.
     * GET /api/v1/mls/regions/code/{regionCode}
     */
    @GetMapping("/code/{regionCode}")
    public ResponseEntity<MLSRegion> getRegionByCode(@PathVariable String regionCode) {
        log.info("Getting MLS region by code: {}", regionCode);
        
        Optional<MLSRegion> region = mlsRegionService.getRegionByCode(regionCode);
        
        return region.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all active MLS regions.
     * GET /api/v1/mls/regions/active
     */
    @GetMapping("/active")
    public ResponseEntity<List<MLSRegion>> getActiveRegions() {
        log.info("Getting all active MLS regions");
        
        List<MLSRegion> regions = mlsRegionService.getActiveRegions();
        
        return ResponseEntity.ok(regions);
    }

    /**
     * Get all MLS regions with pagination.
     * GET /api/v1/mls/regions
     */
    @GetMapping
    public ResponseEntity<Page<MLSRegion>> getAllRegions(Pageable pageable) {
        log.info("Getting all MLS regions with pagination");
        
        List<MLSRegion> regions = mlsRegionService.getAllRegions();
        
        // Convert to page (in real application, you'd use PageableRepository)
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), regions.size());
        Page<MLSRegion> page = new PageImpl<>(
            regions.subList(start, end), 
            pageable, 
            regions.size()
        );
        
        return ResponseEntity.ok(page);
    }

    /**
     * Search MLS regions by criteria.
     * GET /api/v1/mls/regions/search
     */
    @GetMapping("/search")
    public ResponseEntity<List<MLSRegion>> searchRegions(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String regionCode,
            @RequestParam(required = false) Boolean isActive) {
        log.info("Searching MLS regions with criteria: name={}, regionCode={}, isActive={}", 
                name, regionCode, isActive);
        
        List<MLSRegion> regions = mlsRegionService.searchRegions(name, regionCode, isActive);
        
        return ResponseEntity.ok(regions);
    }

    /**
     * Find MLS region by location coordinates.
     * GET /api/v1/mls/regions/location
     */
    @GetMapping("/location")
    public ResponseEntity<MLSRegion> findRegionByLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        log.info("Finding MLS region by location: {}, {}", latitude, longitude);
        
        Optional<MLSRegion> region = mlsRegionService.findRegionByLocation(latitude, longitude);
        
        return region.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Find all MLS regions containing the location.
     * GET /api/v1/mls/regions/location/all
     */
    @GetMapping("/location/all")
    public ResponseEntity<List<MLSRegion>> findRegionsByLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        log.info("Finding all MLS regions by location: {}, {}", latitude, longitude);
        
        List<MLSRegion> regions = mlsRegionService.findRegionsByLocation(latitude, longitude);
        
        return ResponseEntity.ok(regions);
    }

    /**
     * Activate a region.
     * POST /api/v1/mls/regions/{regionId}/activate
     */
    @PostMapping("/{regionId}/activate")
    public ResponseEntity<MLSRegion> activateRegion(@PathVariable UUID regionId) {
        log.info("Activating MLS region: {}", regionId);
        
        MLSRegion region = mlsRegionService.activateRegion(regionId);
        
        return ResponseEntity.ok(region);
    }

    /**
     * Deactivate a region.
     * POST /api/v1/mls/regions/{regionId}/deactivate
     */
    @PostMapping("/{regionId}/deactivate")
    public ResponseEntity<MLSRegion> deactivateRegion(@PathVariable UUID regionId) {
        log.info("Deactivating MLS region: {}", regionId);
        
        MLSRegion region = mlsRegionService.deactivateRegion(regionId);
        
        return ResponseEntity.ok(region);
    }

    /**
     * Delete a region.
     * DELETE /api/v1/mls/regions/{regionId}
     */
    @DeleteMapping("/{regionId}")
    public ResponseEntity<Void> deleteRegion(@PathVariable UUID regionId) {
        log.info("Deleting MLS region: {}", regionId);
        
        mlsRegionService.deleteRegion(regionId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if region code is available.
     * GET /api/v1/mls/regions/check-code/{regionCode}
     */
    @GetMapping("/check-code/{regionCode}")
    public ResponseEntity<RegionCodeAvailabilityResponse> checkRegionCodeAvailability(
            @PathVariable String regionCode) {
        log.info("Checking region code availability: {}", regionCode);
        
        boolean available = mlsRegionService.isRegionCodeAvailable(regionCode);
        
        RegionCodeAvailabilityResponse response = RegionCodeAvailabilityResponse.builder()
            .regionCode(regionCode)
            .available(available)
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Generate MLS number for a region.
     * POST /api/v1/mls/regions/{regionId}/generate-mls-number
     */
    @PostMapping("/{regionId}/generate-mls-number")
    public ResponseEntity<MLSNumberResponse> generateMLSNumber(@PathVariable UUID regionId) {
        log.info("Generating MLS number for region: {}", regionId);
        
        String mlsNumber = mlsRegionService.generateMLSNumber(regionId);
        
        MLSNumberResponse response = MLSNumberResponse.builder()
            .regionId(regionId)
            .mlsNumber(mlsNumber)
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get region statistics.
     * GET /api/v1/mls/regions/{regionId}/stats
     */
    @GetMapping("/{regionId}/stats")
    public ResponseEntity<MLSRegionService.RegionStats> getRegionStats(@PathVariable UUID regionId) {
        log.info("Getting region statistics: {}", regionId);
        
        MLSRegionService.RegionStats stats = mlsRegionService.getRegionStats(regionId);
        
        return ResponseEntity.ok(stats);
    }

    // DTOs for requests and responses
    @lombok.Data
    @lombok.Builder
    public static class CreateRegionRequest {
        @jakarta.validation.constraints.NotBlank
        private String regionCode;
        
        @jakarta.validation.constraints.NotBlank
        private String name;
        
        private String description;
        private Double northBoundary;
        private Double southBoundary;
        private Double eastBoundary;
        private Double westBoundary;
        private String listingPrefix;
        private Integer maxListingDays;
        private Boolean requiresApproval;
        private Boolean allowsPrivateRemarks;
        private Integer mandatoryShowingTimeHours;
        private String adminEmail;
        private String adminPhone;
        private String websiteUrl;
    }

    @lombok.Data
    @lombok.Builder
    public static class UpdateRegionRequest {
        @jakarta.validation.constraints.NotBlank
        private String regionCode;
        
        @jakarta.validation.constraints.NotBlank
        private String name;
        
        private String description;
        private Boolean isActive;
        private Double northBoundary;
        private Double southBoundary;
        private Double eastBoundary;
        private Double westBoundary;
        private String listingPrefix;
        private Integer maxListingDays;
        private Boolean requiresApproval;
        private Boolean allowsPrivateRemarks;
        private Integer mandatoryShowingTimeHours;
        private String adminEmail;
        private String adminPhone;
        private String websiteUrl;
    }

    @lombok.Data
    @lombok.Builder
    public static class RegionCodeAvailabilityResponse {
        private String regionCode;
        private Boolean available;
    }

    @lombok.Data
    @lombok.Builder
    public static class MLSNumberResponse {
        private UUID regionId;
        private String mlsNumber;
    }
} 