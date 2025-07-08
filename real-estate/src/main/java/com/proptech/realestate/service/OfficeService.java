package com.proptech.realestate.service;

import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.model.entity.MLSRegion;
import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.repository.OfficeRepository;
import com.proptech.realestate.repository.MLSRegionRepository;
import com.proptech.realestate.repository.MLSMembershipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing real estate offices.
 * Provides business logic for office operations and agent management.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OfficeService {

    private final OfficeRepository officeRepository;
    private final MLSRegionRepository regionRepository;
    private final MLSMembershipRepository membershipRepository;

    /**
     * Create a new office.
     */
    public Office createOffice(CreateOfficeRequest request) {
        log.info("Creating new office: {}", request.getOfficeName());
        
        // Validate region exists
        MLSRegion region = null;
        if (request.getMlsRegionId() != null) {
            region = regionRepository.findById(request.getMlsRegionId())
                .orElseThrow(() -> new EntityNotFoundException("MLS region not found: " + request.getMlsRegionId()));
        }
        
        // Validate unique license number
        if (request.getLicenseNumber() != null && 
            officeRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new IllegalArgumentException("License number already exists: " + request.getLicenseNumber());
        }
        
        // Validate unique email
        if (request.getEmail() != null && 
            officeRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        // Create office
        Office office = new Office();
        office.setMlsRegion(region);
        office.setOfficeName(request.getOfficeName());
        office.setLicenseNumber(request.getLicenseNumber());
        office.setOfficeType(request.getOfficeType());
        office.setPhone(request.getPhone());
        office.setEmail(request.getEmail());
        office.setWebsite(request.getWebsite());
        office.setStreetAddress(request.getStreetAddress());
        office.setCity(request.getCity());
        office.setState(request.getState());
        office.setZipCode(request.getZipCode());
        office.setCountry(request.getCountry() != null ? request.getCountry() : "VN");
        office.setLatitude(request.getLatitude());
        office.setLongitude(request.getLongitude());
        office.setEstablishedDate(request.getEstablishedDate());
        office.setFranchiseName(request.getFranchiseName());
        office.setIsActive(true);
        office.setTotalAgents(0);
        
        Office savedOffice = officeRepository.save(office);
        log.info("Created office with ID: {}", savedOffice.getId());
        
        return savedOffice;
    }

    /**
     * Update an existing office.
     */
    public Office updateOffice(UUID officeId, UpdateOfficeRequest request) {
        log.info("Updating office: {}", officeId);
        
        Office office = officeRepository.findById(officeId)
            .orElseThrow(() -> new EntityNotFoundException("Office not found: " + officeId));
        
        // Validate unique license number if changed
        if (request.getLicenseNumber() != null && 
            !request.getLicenseNumber().equals(office.getLicenseNumber()) &&
            officeRepository.existsByLicenseNumberAndIdNot(request.getLicenseNumber(), officeId)) {
            throw new IllegalArgumentException("License number already exists: " + request.getLicenseNumber());
        }
        
        // Validate unique email if changed
        if (request.getEmail() != null && 
            !request.getEmail().equals(office.getEmail()) &&
            officeRepository.existsByEmailAndIdNot(request.getEmail(), officeId)) {
            throw new IllegalArgumentException("Email already exists: " + request.getEmail());
        }
        
        // Update region if provided
        if (request.getMlsRegionId() != null) {
            MLSRegion region = regionRepository.findById(request.getMlsRegionId())
                .orElseThrow(() -> new EntityNotFoundException("MLS region not found: " + request.getMlsRegionId()));
            office.setMlsRegion(region);
        }
        
        // Update fields
        if (request.getOfficeName() != null) {
            office.setOfficeName(request.getOfficeName());
        }
        if (request.getLicenseNumber() != null) {
            office.setLicenseNumber(request.getLicenseNumber());
        }
        if (request.getOfficeType() != null) {
            office.setOfficeType(request.getOfficeType());
        }
        if (request.getPhone() != null) {
            office.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            office.setEmail(request.getEmail());
        }
        if (request.getWebsite() != null) {
            office.setWebsite(request.getWebsite());
        }
        if (request.getStreetAddress() != null) {
            office.setStreetAddress(request.getStreetAddress());
        }
        if (request.getCity() != null) {
            office.setCity(request.getCity());
        }
        if (request.getState() != null) {
            office.setState(request.getState());
        }
        if (request.getZipCode() != null) {
            office.setZipCode(request.getZipCode());
        }
        if (request.getCountry() != null) {
            office.setCountry(request.getCountry());
        }
        if (request.getLatitude() != null) {
            office.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            office.setLongitude(request.getLongitude());
        }
        if (request.getEstablishedDate() != null) {
            office.setEstablishedDate(request.getEstablishedDate());
        }
        if (request.getFranchiseName() != null) {
            office.setFranchiseName(request.getFranchiseName());
        }
        if (request.getIsActive() != null) {
            office.setIsActive(request.getIsActive());
        }
        
        Office savedOffice = officeRepository.save(office);
        log.info("Updated office: {}", savedOffice.getId());
        
        return savedOffice;
    }

    /**
     * Get office by ID.
     */
    @Transactional(readOnly = true)
    public Office getOfficeById(UUID officeId) {
        return officeRepository.findById(officeId)
            .orElseThrow(() -> new EntityNotFoundException("Office not found: " + officeId));
    }

    /**
     * Get office by license number.
     */
    @Transactional(readOnly = true)
    public Optional<Office> getOfficeByLicenseNumber(String licenseNumber) {
        return officeRepository.findByLicenseNumber(licenseNumber);
    }

    /**
     * Get offices by MLS region.
     */
    @Transactional(readOnly = true)
    public List<Office> getOfficesByRegion(UUID regionId) {
        MLSRegion region = regionRepository.findById(regionId)
            .orElseThrow(() -> new EntityNotFoundException("Region not found: " + regionId));
        
        return officeRepository.findByMlsRegionAndIsActiveTrue(region);
    }

    /**
     * Get offices by franchise.
     */
    @Transactional(readOnly = true)
    public List<Office> getOfficesByFranchise(String franchiseName, UUID regionId) {
        if (regionId != null) {
            MLSRegion region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException("Region not found: " + regionId));
            return officeRepository.findByFranchiseNameAndMlsRegion(franchiseName, region);
        }
        
        return officeRepository.findByFranchiseName(franchiseName);
    }

    /**
     * Search offices by criteria.
     */
    @Transactional(readOnly = true)
    public List<Office> searchOffices(SearchOfficeRequest request) {
        return officeRepository.searchOffices(
            request.getOfficeName(),
            request.getCity(),
            request.getState(),
            request.getOfficeType(),
            request.getRegionId(),
            request.getIsActive()
        );
    }

    /**
     * Find offices near location.
     */
    @Transactional(readOnly = true)
    public List<Office> findOfficesNearLocation(Double latitude, Double longitude, Double radiusKm) {
        // Convert km to degrees (approximate)
        Double radiusDegrees = radiusKm / 111.0; // 1 degree ≈ 111 km
        
        return officeRepository.findNearLocation(latitude, longitude, radiusDegrees);
    }

    /**
     * Get top offices by agent count.
     */
    @Transactional(readOnly = true)
    public List<Office> getTopOfficesByAgentCount(UUID regionId, int limit) {
        if (regionId != null) {
            MLSRegion region = regionRepository.findById(regionId)
                .orElseThrow(() -> new EntityNotFoundException("Region not found: " + regionId));
            return officeRepository.findOfficesOrderedByAgentCountInRegion(region)
                .stream().limit(limit).toList();
        }
        
        return officeRepository.findOfficesOrderedByAgentCount()
            .stream().limit(limit).toList();
    }

    /**
     * Add agent to office.
     */
    public Office addAgentToOffice(UUID officeId, UUID membershipId) {
        log.info("Adding agent to office: {} -> {}", membershipId, officeId);
        
        Office office = getOfficeById(officeId);
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        // Validate membership is for an agent/broker
        if (!membership.getType().name().contains("AGENT") && 
            !membership.getType().name().contains("BROKER")) {
            throw new IllegalArgumentException("Only agents and brokers can be added to offices");
        }
        
        // Validate office is in same region as membership
        if (!office.getMlsRegion().equals(membership.getMlsRegion())) {
            throw new IllegalArgumentException("Office and membership must be in the same MLS region");
        }
        
        // Update membership office
        membership.setOffice(office);
        membershipRepository.save(membership);
        
        // Update office agent count
        office.incrementAgentCount();
        
        return officeRepository.save(office);
    }

    /**
     * Remove agent from office.
     */
    public Office removeAgentFromOffice(UUID officeId, UUID membershipId) {
        log.info("Removing agent from office: {} -> {}", membershipId, officeId);
        
        Office office = getOfficeById(officeId);
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        // Validate membership is currently in this office
        if (!office.equals(membership.getOffice())) {
            throw new IllegalArgumentException("Membership is not in this office");
        }
        
        // Remove membership from office
        membership.setOffice(null);
        membershipRepository.save(membership);
        
        // Update office agent count
        office.decrementAgentCount();
        
        return officeRepository.save(office);
    }

    /**
     * Get office statistics.
     */
    @Transactional(readOnly = true)
    public OfficeStats getOfficeStats(UUID officeId) {
        Office office = getOfficeById(officeId);
        
        // Get active memberships count
        long activeMemberships = membershipRepository.countActiveByOffice(office);
        
        // Get agent count by type
        long agents = membershipRepository.countActiveByTypeAndRegion(
            MLSMembership.MembershipType.AGENT, office.getMlsRegion());
        long brokers = membershipRepository.countActiveByTypeAndRegion(
            MLSMembership.MembershipType.BROKER, office.getMlsRegion());
        
        return OfficeStats.builder()
            .officeId(officeId)
            .officeName(office.getOfficeName())
            .isActive(office.getIsActive())
            .totalAgents(office.getTotalAgents())
            .activeMemberships(activeMemberships)
            .agentsCount(agents)
            .brokersCount(brokers)
            .establishedDate(office.getEstablishedDate())
            .hasLocation(office.hasLocation())
            .build();
    }

    /**
     * Activate office.
     */
    public Office activateOffice(UUID officeId) {
        log.info("Activating office: {}", officeId);
        
        Office office = getOfficeById(officeId);
        office.setIsActive(true);
        
        return officeRepository.save(office);
    }

    /**
     * Deactivate office.
     */
    public Office deactivateOffice(UUID officeId) {
        log.info("Deactivating office: {}", officeId);
        
        Office office = getOfficeById(officeId);
        office.setIsActive(false);
        
        return officeRepository.save(office);
    }

    // DTOs for requests
    @lombok.Data
    @lombok.Builder
    public static class CreateOfficeRequest {
        private UUID mlsRegionId;
        private String officeName;
        private String licenseNumber;
        private Office.OfficeType officeType;
        private String phone;
        private String email;
        private String website;
        private String streetAddress;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private Double latitude;
        private Double longitude;
        private LocalDate establishedDate;
        private String franchiseName;
    }

    @lombok.Data
    @lombok.Builder
    public static class UpdateOfficeRequest {
        private UUID mlsRegionId;
        private String officeName;
        private String licenseNumber;
        private Office.OfficeType officeType;
        private String phone;
        private String email;
        private String website;
        private String streetAddress;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private Double latitude;
        private Double longitude;
        private LocalDate establishedDate;
        private String franchiseName;
        private Boolean isActive;
    }

    @lombok.Data
    @lombok.Builder
    public static class SearchOfficeRequest {
        private String officeName;
        private String city;
        private String state;
        private Office.OfficeType officeType;
        private UUID regionId;
        private Boolean isActive;
    }

    @lombok.Data
    @lombok.Builder
    public static class OfficeStats {
        private UUID officeId;
        private String officeName;
        private Boolean isActive;
        private Integer totalAgents;
        private Long activeMemberships;
        private Long agentsCount;
        private Long brokersCount;
        private LocalDate establishedDate;
        private Boolean hasLocation;
    }
} 