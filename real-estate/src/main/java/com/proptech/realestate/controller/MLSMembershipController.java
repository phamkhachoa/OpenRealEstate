package com.proptech.realestate.controller;

import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.service.MLSMembershipService;
import com.proptech.realestate.service.MLSMembershipService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing MLS memberships.
 * Provides endpoints for agent and broker management operations.
 */
@RestController
@RequestMapping("/api/memberships")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class MLSMembershipController {

    private final MLSMembershipService membershipService;

    /**
     * Create a new MLS membership.
     */
    @PostMapping
    public ResponseEntity<MLSMembership> createMembership(@Valid @RequestBody CreateMembershipRequest request) {
        log.info("Creating membership for user: {} in region: {}", 
                request.getUserId(), request.getMlsRegionId());
        
        MLSMembership membership = membershipService.createMembership(request);
        return new ResponseEntity<>(membership, HttpStatus.CREATED);
    }

    /**
     * Get membership by ID.
     */
    @GetMapping("/{membershipId}")
    public ResponseEntity<MLSMembership> getMembershipById(@PathVariable UUID membershipId) {
        log.info("Getting membership by ID: {}", membershipId);
        
        MLSMembership membership = membershipService.getMembershipById(membershipId);
        return ResponseEntity.ok(membership);
    }

    /**
     * Update membership.
     */
    @PutMapping("/{membershipId}")
    public ResponseEntity<MLSMembership> updateMembership(
            @PathVariable UUID membershipId, 
            @Valid @RequestBody UpdateMembershipRequest request) {
        log.info("Updating membership: {}", membershipId);
        
        MLSMembership membership = membershipService.updateMembership(membershipId, request);
        return ResponseEntity.ok(membership);
    }

    /**
     * Get memberships by user ID.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MLSMembership>> getMembershipsByUserId(@PathVariable UUID userId) {
        log.info("Getting memberships for user: {}", userId);
        
        List<MLSMembership> memberships = membershipService.getMembershipsByUserId(userId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get memberships by MLS region.
     */
    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<MLSMembership>> getMembershipsByRegion(
            @PathVariable UUID regionId,
            @RequestParam(required = false) Boolean isActive) {
        log.info("Getting memberships for region: {}, active: {}", regionId, isActive);
        
        List<MLSMembership> memberships = membershipService.getMembershipsByRegion(regionId, isActive);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get memberships by office.
     */
    @GetMapping("/office/{officeId}")
    public ResponseEntity<List<MLSMembership>> getMembershipsByOffice(@PathVariable UUID officeId) {
        log.info("Getting memberships for office: {}", officeId);
        
        List<MLSMembership> memberships = membershipService.getMembershipsByOffice(officeId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get memberships by type.
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<List<MLSMembership>> getMembershipsByType(
            @PathVariable MLSMembership.MembershipType type,
            @RequestParam(required = false) UUID regionId) {
        log.info("Getting memberships of type: {} in region: {}", type, regionId);
        
        List<MLSMembership> memberships = membershipService.getMembershipsByType(type, regionId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Search memberships by criteria.
     */
    @PostMapping("/search")
    public ResponseEntity<List<MLSMembership>> searchMemberships(@RequestBody SearchMembershipRequest request) {
        log.info("Searching memberships with criteria: {}", request);
        
        List<MLSMembership> memberships = membershipService.searchMemberships(request);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Activate membership.
     */
    @PutMapping("/{membershipId}/activate")
    public ResponseEntity<MLSMembership> activateMembership(@PathVariable UUID membershipId) {
        log.info("Activating membership: {}", membershipId);
        
        MLSMembership membership = membershipService.activateMembership(membershipId);
        return ResponseEntity.ok(membership);
    }

    /**
     * Suspend membership.
     */
    @PutMapping("/{membershipId}/suspend")
    public ResponseEntity<MLSMembership> suspendMembership(
            @PathVariable UUID membershipId,
            @RequestParam String reason) {
        log.info("Suspending membership: {} with reason: {}", membershipId, reason);
        
        MLSMembership membership = membershipService.suspendMembership(membershipId, reason);
        return ResponseEntity.ok(membership);
    }

    /**
     * Terminate membership.
     */
    @PutMapping("/{membershipId}/terminate")
    public ResponseEntity<MLSMembership> terminateMembership(
            @PathVariable UUID membershipId,
            @RequestParam String reason) {
        log.info("Terminating membership: {} with reason: {}", membershipId, reason);
        
        MLSMembership membership = membershipService.terminateMembership(membershipId, reason);
        return ResponseEntity.ok(membership);
    }

    /**
     * Renew membership.
     */
    @PutMapping("/{membershipId}/renew")
    public ResponseEntity<MLSMembership> renewMembership(
            @PathVariable UUID membershipId,
            @RequestParam int months) {
        log.info("Renewing membership: {} for {} months", membershipId, months);
        
        MLSMembership membership = membershipService.renewMembership(membershipId, months);
        return ResponseEntity.ok(membership);
    }

    /**
     * Update membership fees.
     */
    @PutMapping("/{membershipId}/fees")
    public ResponseEntity<MLSMembership> updateMembershipFees(
            @PathVariable UUID membershipId,
            @RequestParam BigDecimal setupFee,
            @RequestParam BigDecimal monthlyFee) {
        log.info("Updating fees for membership: {} - setup: {}, monthly: {}", 
                membershipId, setupFee, monthlyFee);
        
        MLSMembership membership = membershipService.updateMembershipFees(membershipId, setupFee, monthlyFee);
        return ResponseEntity.ok(membership);
    }

    /**
     * Get membership statistics by region.
     */
    @GetMapping("/stats/region/{regionId}")
    public ResponseEntity<MembershipStats> getMembershipStatsByRegion(@PathVariable UUID regionId) {
        log.info("Getting membership statistics for region: {}", regionId);
        
        MembershipStats stats = membershipService.getMembershipStatsByRegion(regionId);
        return ResponseEntity.ok(stats);
    }

    /**
     * Get expiring memberships.
     */
    @GetMapping("/expiring")
    public ResponseEntity<List<MLSMembership>> getExpiringMemberships(
            @RequestParam(defaultValue = "30") int withinDays,
            @RequestParam(required = false) UUID regionId) {
        log.info("Getting memberships expiring within {} days in region: {}", withinDays, regionId);
        
        List<MLSMembership> memberships = membershipService.getExpiringMemberships(withinDays, regionId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Get pending memberships.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<MLSMembership>> getPendingMemberships(
            @RequestParam(required = false) UUID regionId) {
        log.info("Getting pending memberships in region: {}", regionId);
        
        List<MLSMembership> memberships = membershipService.getPendingMemberships(regionId);
        return ResponseEntity.ok(memberships);
    }

    /**
     * Approve pending membership.
     */
    @PutMapping("/{membershipId}/approve")
    public ResponseEntity<MLSMembership> approveMembership(
            @PathVariable UUID membershipId,
            @RequestParam UUID approvedByUserId) {
        log.info("Approving membership: {} by user: {}", membershipId, approvedByUserId);
        
        MLSMembership membership = membershipService.approveMembership(membershipId, approvedByUserId);
        return ResponseEntity.ok(membership);
    }

    /**
     * Check membership status.
     */
    @GetMapping("/{membershipId}/status")
    public ResponseEntity<MembershipStatusResponse> checkMembershipStatus(@PathVariable UUID membershipId) {
        log.info("Checking status for membership: {}", membershipId);
        
        MLSMembership membership = membershipService.getMembershipById(membershipId);
        
        MembershipStatusResponse response = MembershipStatusResponse.builder()
            .membershipId(membershipId)
            .status(membership.getStatus())
            .isActive(membership.getIsActive())
            .expiryDate(membership.getExpiryDate())
            .daysUntilExpiry(membership.getDaysUntilExpiry())
            .isExpired(membership.isExpired())
            .type(membership.getType())
            .regionName(membership.getMlsRegion().getRegionName())
            .build();
        
        return ResponseEntity.ok(response);
    }

    /**
     * Get membership by MLS number.
     */
    @GetMapping("/mls/{mlsNumber}")
    public ResponseEntity<MLSMembership> getMembershipByMlsNumber(@PathVariable String mlsNumber) {
        log.info("Getting membership by MLS number: {}", mlsNumber);
        
        MLSMembership membership = membershipService.getMembershipByMlsNumber(mlsNumber);
        return ResponseEntity.ok(membership);
    }

    /**
     * Transfer membership to new office.
     */
    @PutMapping("/{membershipId}/transfer")
    public ResponseEntity<MLSMembership> transferMembership(
            @PathVariable UUID membershipId,
            @RequestParam UUID newOfficeId) {
        log.info("Transferring membership: {} to office: {}", membershipId, newOfficeId);
        
        MLSMembership membership = membershipService.transferMembership(membershipId, newOfficeId);
        return ResponseEntity.ok(membership);
    }

    /**
     * Get agents by supervising broker.
     */
    @GetMapping("/broker/{brokerId}/agents")
    public ResponseEntity<List<MLSMembership>> getAgentsBySupervisingBroker(@PathVariable UUID brokerId) {
        log.info("Getting agents supervised by broker: {}", brokerId);
        
        List<MLSMembership> agents = membershipService.getAgentsBySupervisingBroker(brokerId);
        return ResponseEntity.ok(agents);
    }

    /**
     * Validate MLS number.
     */
    @GetMapping("/validate/{mlsNumber}")
    public ResponseEntity<Boolean> validateMlsNumber(@PathVariable String mlsNumber) {
        log.info("Validating MLS number: {}", mlsNumber);
        
        boolean isValid = membershipService.validateMlsNumber(mlsNumber);
        return ResponseEntity.ok(isValid);
    }

    // Response DTOs
    @lombok.Data
    @lombok.Builder
    public static class MembershipStatusResponse {
        private UUID membershipId;
        private MLSMembership.MembershipStatus status;
        private Boolean isActive;
        private LocalDate expiryDate;
        private Long daysUntilExpiry;
        private Boolean isExpired;
        private MLSMembership.MembershipType type;
        private String regionName;
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