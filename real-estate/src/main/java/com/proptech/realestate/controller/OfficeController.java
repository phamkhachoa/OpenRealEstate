package com.proptech.realestate.controller;

import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.service.OfficeService;
import com.proptech.realestate.service.OfficeService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for managing real estate offices.
 * Provides endpoints for office CRUD operations and business logic.
 */
@RestController
@RequestMapping("/api/offices")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OfficeController {

    private final OfficeService officeService;

    /**
     * Create a new office.
     */
    @PostMapping
    public ResponseEntity<Office> createOffice(@Valid @RequestBody CreateOfficeRequest request) {
        log.info("Creating office: {}", request.getOfficeName());
        
        Office office = officeService.createOffice(request);
        return new ResponseEntity<>(office, HttpStatus.CREATED);
    }

    /**
     * Get office by ID.
     */
    @GetMapping("/{officeId}")
    public ResponseEntity<Office> getOfficeById(@PathVariable UUID officeId) {
        log.info("Getting office by ID: {}", officeId);
        
        Office office = officeService.getOfficeById(officeId);
        return ResponseEntity.ok(office);
    }

    /**
     * Update office.
     */
    @PutMapping("/{officeId}")
    public ResponseEntity<Office> updateOffice(
            @PathVariable UUID officeId, 
            @Valid @RequestBody UpdateOfficeRequest request) {
        log.info("Updating office: {}", officeId);
        
        Office office = officeService.updateOffice(officeId, request);
        return ResponseEntity.ok(office);
    }

    /**
     * Get office by license number.
     */
    @GetMapping("/license/{licenseNumber}")
    public ResponseEntity<Office> getOfficeByLicenseNumber(@PathVariable String licenseNumber) {
        log.info("Getting office by license number: {}", licenseNumber);
        
        Optional<Office> office = officeService.getOfficeByLicenseNumber(licenseNumber);
        return office.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get offices by MLS region.
     */
    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<Office>> getOfficesByRegion(@PathVariable UUID regionId) {
        log.info("Getting offices by region: {}", regionId);
        
        List<Office> offices = officeService.getOfficesByRegion(regionId);
        return ResponseEntity.ok(offices);
    }

    /**
     * Get offices by franchise.
     */
    @GetMapping("/franchise/{franchiseName}")
    public ResponseEntity<List<Office>> getOfficesByFranchise(
            @PathVariable String franchiseName,
            @RequestParam(required = false) UUID regionId) {
        log.info("Getting offices by franchise: {} in region: {}", franchiseName, regionId);
        
        List<Office> offices = officeService.getOfficesByFranchise(franchiseName, regionId);
        return ResponseEntity.ok(offices);
    }

    /**
     * Search offices by criteria.
     */
    @PostMapping("/search")
    public ResponseEntity<List<Office>> searchOffices(@RequestBody SearchOfficeRequest request) {
        log.info("Searching offices with criteria: {}", request);
        
        List<Office> offices = officeService.searchOffices(request);
        return ResponseEntity.ok(offices);
    }

    /**
     * Find offices near location.
     */
    @GetMapping("/near")
    public ResponseEntity<List<Office>> findOfficesNearLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "10.0") Double radiusKm) {
        log.info("Finding offices near location: {}, {} within {} km", 
                latitude, longitude, radiusKm);
        
        List<Office> offices = officeService.findOfficesNearLocation(latitude, longitude, radiusKm);
        return ResponseEntity.ok(offices);
    }

    /**
     * Get top offices by agent count.
     */
    @GetMapping("/top")
    public ResponseEntity<List<Office>> getTopOfficesByAgentCount(
            @RequestParam(required = false) UUID regionId,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Getting top {} offices by agent count in region: {}", limit, regionId);
        
        List<Office> offices = officeService.getTopOfficesByAgentCount(regionId, limit);
        return ResponseEntity.ok(offices);
    }

    /**
     * Add agent to office.
     */
    @PostMapping("/{officeId}/agents/{membershipId}")
    public ResponseEntity<Office> addAgentToOffice(
            @PathVariable UUID officeId,
            @PathVariable UUID membershipId) {
        log.info("Adding agent {} to office {}", membershipId, officeId);
        
        Office office = officeService.addAgentToOffice(officeId, membershipId);
        return ResponseEntity.ok(office);
    }

    /**
     * Remove agent from office.
     */
    @DeleteMapping("/{officeId}/agents/{membershipId}")
    public ResponseEntity<Office> removeAgentFromOffice(
            @PathVariable UUID officeId,
            @PathVariable UUID membershipId) {
        log.info("Removing agent {} from office {}", membershipId, officeId);
        
        Office office = officeService.removeAgentFromOffice(officeId, membershipId);
        return ResponseEntity.ok(office);
    }

    /**
     * Get office statistics.
     */
    @GetMapping("/{officeId}/stats")
    public ResponseEntity<OfficeStats> getOfficeStats(@PathVariable UUID officeId) {
        log.info("Getting statistics for office: {}", officeId);
        
        OfficeStats stats = officeService.getOfficeStats(officeId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Activate office.
     */
    @PutMapping("/{officeId}/activate")
    public ResponseEntity<Office> activateOffice(@PathVariable UUID officeId) {
        log.info("Activating office: {}", officeId);
        
        Office office = officeService.activateOffice(officeId);
        return ResponseEntity.ok(office);
    }

    /**
     * Deactivate office.
     */
    @PutMapping("/{officeId}/deactivate")
    public ResponseEntity<Office> deactivateOffice(@PathVariable UUID officeId) {
        log.info("Deactivating office: {}", officeId);
        
        Office office = officeService.deactivateOffice(officeId);
        return ResponseEntity.ok(office);
    }

    /**
     * Get all active offices.
     */
    @GetMapping
    public ResponseEntity<List<Office>> getAllActiveOffices() {
        log.info("Getting all active offices");
        
        SearchOfficeRequest request = SearchOfficeRequest.builder()
            .isActive(true)
            .build();
        
        List<Office> offices = officeService.searchOffices(request);
        return ResponseEntity.ok(offices);
    }

    /**
     * Count offices by region.
     */
    @GetMapping("/count/region/{regionId}")
    public ResponseEntity<Long> countOfficesByRegion(@PathVariable UUID regionId) {
        log.info("Counting offices in region: {}", regionId);
        
        List<Office> offices = officeService.getOfficesByRegion(regionId);
        return ResponseEntity.ok((long) offices.size());
    }

    /**
     * Get offices by city.
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<Office>> getOfficesByCity(
            @PathVariable String city,
            @RequestParam(required = false) String state) {
        log.info("Getting offices in city: {}, state: {}", city, state);
        
        SearchOfficeRequest request = SearchOfficeRequest.builder()
            .city(city)
            .state(state)
            .isActive(true)
            .build();
        
        List<Office> offices = officeService.searchOffices(request);
        return ResponseEntity.ok(offices);
    }

    /**
     * Get offices by type.
     */
    @GetMapping("/type/{officeType}")
    public ResponseEntity<List<Office>> getOfficesByType(
            @PathVariable Office.OfficeType officeType,
            @RequestParam(required = false) UUID regionId) {
        log.info("Getting offices of type: {} in region: {}", officeType, regionId);
        
        SearchOfficeRequest request = SearchOfficeRequest.builder()
            .officeType(officeType)
            .regionId(regionId)
            .isActive(true)
            .build();
        
        List<Office> offices = officeService.searchOffices(request);
        return ResponseEntity.ok(offices);
    }

    /**
     * Check if office name is available.
     */
    @GetMapping("/check-name")
    public ResponseEntity<Boolean> checkOfficeNameAvailability(@RequestParam String officeName) {
        log.info("Checking office name availability: {}", officeName);
        
        SearchOfficeRequest request = SearchOfficeRequest.builder()
            .officeName(officeName)
            .build();
        
        List<Office> offices = officeService.searchOffices(request);
        boolean isAvailable = offices.isEmpty();
        
        return ResponseEntity.ok(isAvailable);
    }

    /**
     * Exception handlers.
     */
    @ExceptionHandler(jakarta.persistence.EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(jakarta.persistence.EntityNotFoundException ex) {
        log.error("Entity not found: {}", ex.getMessage());
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An unexpected error occurred");
    }
} 