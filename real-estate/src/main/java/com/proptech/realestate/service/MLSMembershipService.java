package com.proptech.realestate.service;

import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.model.entity.MLSRegion;
import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.model.entity.User;
import com.proptech.realestate.repository.MLSMembershipRepository;
import com.proptech.realestate.repository.MLSRegionRepository;
import com.proptech.realestate.repository.OfficeRepository;
import com.proptech.realestate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class for managing MLS memberships.
 * Provides business logic for membership operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MLSMembershipService {

    private final MLSMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final MLSRegionRepository regionRepository;
    private final OfficeRepository officeRepository;

    /**
     * Create a new MLS membership.
     */
    public MLSMembership createMembership(CreateMembershipRequest request) {
        log.info("Creating new MLS membership for user: {}", request.getUserId());
        
        // Validate user exists
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("User not found: " + request.getUserId()));
        
        // Validate region exists
        MLSRegion region = regionRepository.findById(request.getRegionId())
            .orElseThrow(() -> new EntityNotFoundException("MLS region not found: " + request.getRegionId()));
        
        // Check if user already has active membership in this region
        if (membershipRepository.hasActiveMembershipInRegion(user, region)) {
            throw new IllegalArgumentException("User already has active membership in this region");
        }
        
        // Validate office if provided
        Office office = null;
        if (request.getOfficeId() != null) {
            office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new EntityNotFoundException("Office not found: " + request.getOfficeId()));
            
            // Ensure office is in the same region
            if (!office.getMlsRegion().equals(region)) {
                throw new IllegalArgumentException("Office must be in the same MLS region");
            }
        }
        
        // Create membership
        MLSMembership membership = new MLSMembership();
        membership.setUser(user);
        membership.setMlsRegion(region);
        membership.setType(request.getType());
        membership.setStatus(MLSMembership.MembershipStatus.PENDING);
        membership.setStartDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now());
        membership.setEndDate(request.getEndDate());
        membership.setAnnualFee(request.getAnnualFee());
        membership.setOffice(office);
        membership.setLicenseNumber(request.getLicenseNumber());
        membership.setLicenseState(request.getLicenseState());
        membership.setLicenseExpiryDate(request.getLicenseExpiryDate());
        
        // Generate membership number
        membership.setMembershipNumber(membership.generateMembershipNumber());
        
        // Set default permissions based on type
        membership.setDefaultPermissions();
        
        // Set billing info
        membership.setBillingAddress(request.getBillingAddress());
        membership.setPaymentMethod(request.getPaymentMethod());
        
        MLSMembership savedMembership = membershipRepository.save(membership);
        log.info("Created MLS membership with ID: {}", savedMembership.getId());
        
        return savedMembership;
    }

    /**
     * Update an existing membership.
     */
    public MLSMembership updateMembership(UUID membershipId, UpdateMembershipRequest request) {
        log.info("Updating MLS membership: {}", membershipId);
        
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        // Update basic fields
        if (request.getType() != null) {
            membership.setType(request.getType());
            membership.setDefaultPermissions(); // Reset permissions based on new type
        }
        
        if (request.getStatus() != null) {
            membership.setStatus(request.getStatus());
        }
        
        if (request.getEndDate() != null) {
            membership.setEndDate(request.getEndDate());
        }
        
        if (request.getAnnualFee() != null) {
            membership.setAnnualFee(request.getAnnualFee());
        }
        
        // Update office if provided
        if (request.getOfficeId() != null) {
            Office office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new EntityNotFoundException("Office not found: " + request.getOfficeId()));
            
            // Ensure office is in the same region
            if (!office.getMlsRegion().equals(membership.getMlsRegion())) {
                throw new IllegalArgumentException("Office must be in the same MLS region");
            }
            
            membership.setOffice(office);
        }
        
        // Update license info
        if (request.getLicenseNumber() != null) {
            membership.setLicenseNumber(request.getLicenseNumber());
        }
        
        if (request.getLicenseState() != null) {
            membership.setLicenseState(request.getLicenseState());
        }
        
        if (request.getLicenseExpiryDate() != null) {
            membership.setLicenseExpiryDate(request.getLicenseExpiryDate());
        }
        
        // Update billing info
        if (request.getBillingAddress() != null) {
            membership.setBillingAddress(request.getBillingAddress());
        }
        
        if (request.getPaymentMethod() != null) {
            membership.setPaymentMethod(request.getPaymentMethod());
        }
        
        MLSMembership savedMembership = membershipRepository.save(membership);
        log.info("Updated MLS membership: {}", savedMembership.getId());
        
        return savedMembership;
    }

    /**
     * Activate a membership.
     */
    public MLSMembership activateMembership(UUID membershipId) {
        log.info("Activating MLS membership: {}", membershipId);
        
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        // Validate requirements for activation
        if (!membership.getTrainingCompleted()) {
            throw new IllegalStateException("Training must be completed before activation");
        }
        
        if (!membership.getAgreementAccepted()) {
            throw new IllegalStateException("Agreement must be accepted before activation");
        }
        
        if (membership.getLicenseExpiryDate() != null && 
            membership.getLicenseExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("License is expired");
        }
        
        membership.setStatus(MLSMembership.MembershipStatus.ACTIVE);
        
        return membershipRepository.save(membership);
    }

    /**
     * Suspend a membership.
     */
    public MLSMembership suspendMembership(UUID membershipId) {
        log.info("Suspending MLS membership: {}", membershipId);
        
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        membership.setStatus(MLSMembership.MembershipStatus.SUSPENDED);
        
        return membershipRepository.save(membership);
    }

    /**
     * Terminate a membership.
     */
    public MLSMembership terminateMembership(UUID membershipId) {
        log.info("Terminating MLS membership: {}", membershipId);
        
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        membership.setStatus(MLSMembership.MembershipStatus.TERMINATED);
        
        return membershipRepository.save(membership);
    }

    /**
     * Complete training for a membership.
     */
    public MLSMembership completeTraining(UUID membershipId) {
        log.info("Completing training for membership: {}", membershipId);
        
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        membership.setTrainingCompleted(true);
        membership.setTrainingCompletionDate(LocalDate.now());
        
        return membershipRepository.save(membership);
    }

    /**
     * Accept agreement for a membership.
     */
    public MLSMembership acceptAgreement(UUID membershipId) {
        log.info("Accepting agreement for membership: {}", membershipId);
        
        MLSMembership membership = membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
        
        membership.setAgreementAccepted(true);
        membership.setAgreementAcceptedDate(LocalDateTime.now());
        
        return membershipRepository.save(membership);
    }

    /**
     * Get membership by ID.
     */
    @Transactional(readOnly = true)
    public MLSMembership getMembershipById(UUID membershipId) {
        return membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("Membership not found: " + membershipId));
    }

    /**
     * Get membership by membership number.
     */
    @Transactional(readOnly = true)
    public Optional<MLSMembership> getMembershipByNumber(String membershipNumber) {
        return membershipRepository.findByMembershipNumber(membershipNumber);
    }

    /**
     * Get active memberships for a user.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getActiveMembershipsForUser(UUID userId) {
        return membershipRepository.findActiveByUserId(userId);
    }

    /**
     * Get all memberships for a user.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsForUser(UUID userId) {
        return membershipRepository.findByUserIdAndStatus(userId, null);
    }

    /**
     * Get memberships by region.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsByRegion(UUID regionId) {
        MLSRegion region = regionRepository.findById(regionId)
            .orElseThrow(() -> new EntityNotFoundException("Region not found: " + regionId));
        
        return membershipRepository.findByMlsRegion(region);
    }

    /**
     * Get memberships by office.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsByOffice(UUID officeId) {
        Office office = officeRepository.findById(officeId)
            .orElseThrow(() -> new EntityNotFoundException("Office not found: " + officeId));
        
        return membershipRepository.findByOffice(office);
    }

    /**
     * Find memberships expiring soon.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> findExpiringSoon(int daysAhead) {
        LocalDate expiryDate = LocalDate.now().plusDays(daysAhead);
        return membershipRepository.findExpiringSoon(expiryDate);
    }

    /**
     * Find expired memberships.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> findExpiredMemberships() {
        return membershipRepository.findExpired(LocalDate.now());
    }

    /**
     * Search memberships by criteria.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> searchMemberships(SearchMembershipRequest request) {
        return membershipRepository.searchMemberships(
            request.getUserId(),
            request.getRegionId(),
            request.getOfficeId(),
            request.getType(),
            request.getStatus()
        );
    }

    // DTOs for requests
    @lombok.Data
    @lombok.Builder
    public static class CreateMembershipRequest {
        private UUID userId;
        private UUID regionId;
        private UUID officeId;
        private MLSMembership.MembershipType type;
        private LocalDate startDate;
        private LocalDate endDate;
        private BigDecimal annualFee;
        private String licenseNumber;
        private String licenseState;
        private LocalDate licenseExpiryDate;
        private String billingAddress;
        private String paymentMethod;
    }

    @lombok.Data
    @lombok.Builder
    public static class UpdateMembershipRequest {
        private MLSMembership.MembershipType type;
        private MLSMembership.MembershipStatus status;
        private LocalDate endDate;
        private BigDecimal annualFee;
        private UUID officeId;
        private String licenseNumber;
        private String licenseState;
        private LocalDate licenseExpiryDate;
        private String billingAddress;
        private String paymentMethod;
    }

    @lombok.Data
    @lombok.Builder
    public static class SearchMembershipRequest {
        private UUID userId;
        private UUID regionId;
        private UUID officeId;
        private MLSMembership.MembershipType type;
        private MLSMembership.MembershipStatus status;
    }
} 