package com.proptech.realestate.service;

import com.proptech.realestate.dto.CreateMembershipRequest;
import com.proptech.realestate.dto.MembershipStats;
import com.proptech.realestate.dto.UpdateMembershipRequest;
import com.proptech.realestate.model.entity.MLSMembership;
import com.proptech.realestate.model.entity.MLSRegion;
import com.proptech.realestate.model.entity.Office;
import com.proptech.realestate.model.entity.User;
import com.proptech.realestate.repository.MLSMembershipRepository;
import com.proptech.realestate.repository.MLSRegionRepository;
import com.proptech.realestate.repository.OfficeRepository;
import com.proptech.realestate.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final MLSRegionRepository mlsRegionRepository;
    private final OfficeRepository officeRepository;

    /**
     * Create a new MLS membership.
     */
    public MLSMembership createMembership(CreateMembershipRequest request) {
        log.info("Creating new MLS membership for user: {}", request.getUserId());
        
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + request.getUserId()));
        
        MLSRegion region = mlsRegionRepository.findById(request.getRegionId())
            .orElseThrow(() -> new EntityNotFoundException("MLSRegion not found with id: " + request.getRegionId()));
        
        Office office = null;
        if (request.getOfficeId() != null) {
            office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new EntityNotFoundException("Office not found with id: " + request.getOfficeId()));
        }
        
        MLSMembership membership = new MLSMembership();
        membership.setUser(user);
        membership.setMlsRegion(region);
        membership.setOffice(office);
        membership.setType(request.getType());
        membership.setStatus(MLSMembership.MembershipStatus.PENDING);
        membership.setStartDate(LocalDate.now());
        membership.setExpiryDate(LocalDate.now().plusYears(1));
        membership.setLicenseNumber(request.getLicenseNumber());
        membership.setMembershipNumber(generateMlsNumber(region, request.getType()));
        
        return membershipRepository.save(membership);
    }

    /**
     * Update an existing membership.
     */
    public MLSMembership updateMembership(UUID membershipId, UpdateMembershipRequest request) {
        MLSMembership membership = getMembershipById(membershipId);
        membership.setLicenseNumber(request.getLicenseNumber());
        if (request.getOfficeId() != null) {
            Office office = officeRepository.findById(request.getOfficeId())
                .orElseThrow(() -> new EntityNotFoundException("Office not found with id: " + request.getOfficeId()));
            membership.setOffice(office);
        }
        if(request.getStatus() != null) {
            membership.setStatus(request.getStatus());
        }
        if(request.getType() != null) {
            membership.setType(request.getType());
        }
        
        return membershipRepository.save(membership);
    }

    /**
     * Get membership by ID.
     */
    @Transactional(readOnly = true)
    public MLSMembership getMembershipById(UUID membershipId) {
        return membershipRepository.findById(membershipId)
            .orElseThrow(() -> new EntityNotFoundException("MLSMembership not found with id: " + membershipId));
    }

    /**
     * Approve a membership.
     */
    public MLSMembership approveMembership(UUID membershipId, UUID approvedByUserId) {
        MLSMembership membership = getMembershipById(membershipId);
        User approver = userRepository.findById(approvedByUserId)
            .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + approvedByUserId));
        membership.setStatus(MLSMembership.MembershipStatus.ACTIVE);
        membership.setApprovedBy(approver);
        membership.setApprovedAt(LocalDateTime.now());
        return membershipRepository.save(membership);
    }

    /**
     * Suspend a membership.
     */
    public MLSMembership suspendMembership(UUID membershipId, String reason) {
        MLSMembership membership = getMembershipById(membershipId);
        membership.setStatus(MLSMembership.MembershipStatus.SUSPENDED);
        return membershipRepository.save(membership);
    }

    /**
     * Terminate a membership.
     */
    public MLSMembership terminateMembership(UUID membershipId, String reason) {
        MLSMembership membership = getMembershipById(membershipId);
        membership.setStatus(MLSMembership.MembershipStatus.TERMINATED);
        return membershipRepository.save(membership);
    }

    /**
     * Renew a membership.
     */
    public MLSMembership renewMembership(UUID membershipId, int months) {
        MLSMembership membership = getMembershipById(membershipId);
        membership.setExpiryDate(membership.getExpiryDate() != null ? membership.getExpiryDate().plusMonths(months) : LocalDate.now().plusMonths(months));
        membership.setStatus(MLSMembership.MembershipStatus.ACTIVE);
        return membershipRepository.save(membership);
    }

    /**
     * Update membership fees.
     */
    public MLSMembership updateMembershipFees(UUID membershipId, BigDecimal setupFee, BigDecimal monthlyFee) {
        MLSMembership membership = getMembershipById(membershipId);
        // Assuming fee fields exist on MLSMembership
        // membership.setSetupFee(setupFee);
        // membership.setMonthlyFee(monthlyFee);
        return membershipRepository.save(membership);
    }

    /**
     * Get membership stats by region.
     */
    @Transactional(readOnly = true)
    public MembershipStats getMembershipStatsByRegion(UUID regionId) {
        MLSRegion region = mlsRegionRepository.findById(regionId)
            .orElseThrow(() -> new EntityNotFoundException("MLSRegion not found with id: " + regionId));

        long totalMembers = membershipRepository.countByMlsRegion_Id(regionId);
        long activeMembers = membershipRepository.countByMlsRegion_IdAndStatus(regionId, MLSMembership.MembershipStatus.ACTIVE);
        long pendingMembers = membershipRepository.countByMlsRegion_IdAndStatus(regionId, MLSMembership.MembershipStatus.PENDING);
        long suspendedMembers = membershipRepository.countByMlsRegion_IdAndStatus(regionId, MLSMembership.MembershipStatus.SUSPENDED);
        long terminatedMembers = membershipRepository.countByMlsRegion_IdAndStatus(regionId, MLSMembership.MembershipStatus.TERMINATED);
        long expiredMembers = membershipRepository.countByMlsRegion_IdAndStatus(regionId, MLSMembership.MembershipStatus.EXPIRED);
        long brokers = membershipRepository.countByMlsRegion_IdAndType(regionId, MLSMembership.MembershipType.BROKER);
        long agents = membershipRepository.countByMlsRegion_IdAndType(regionId, MLSMembership.MembershipType.AGENT);

        return MembershipStats.builder()
            .regionId(regionId)
            .regionName(region.getRegionName())
            .totalMembers(totalMembers)
            .activeMembers(activeMembers)
            .pendingMembers(pendingMembers)
            .suspendedMembers(suspendedMembers)
            .terminatedMembers(terminatedMembers)
            .expiredMembers(expiredMembers)
            .brokers(brokers)
            .agents(agents)
            .build();
    }

    /**
     * Get expiring memberships.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getExpiringMemberships(int withinDays, UUID regionId) {
        LocalDate expiryDate = LocalDate.now().plusDays(withinDays);
        return membershipRepository.findExpiringMemberships(expiryDate, regionId);
    }

    /**
     * Get pending memberships.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getPendingMemberships(UUID regionId) {
        if (regionId != null) {
            return membershipRepository.findByMlsRegion_IdAndStatus(regionId, MLSMembership.MembershipStatus.PENDING);
        }
        return membershipRepository.findByStatus(MLSMembership.MembershipStatus.PENDING);
    }

    /**
     * Get membership by MLS number.
     */
    @Transactional(readOnly = true)
    public MLSMembership getMembershipByMlsNumber(String mlsNumber) {
        return membershipRepository.findByMembershipNumber(mlsNumber)
            .orElseThrow(() -> new EntityNotFoundException("MLSMembership not found with MLS number: " + mlsNumber));
    }

    /**
     * Transfer a membership.
     */
    public MLSMembership transferMembership(UUID membershipId, UUID newOfficeId) {
        MLSMembership membership = getMembershipById(membershipId);
        Office newOffice = officeRepository.findById(newOfficeId)
            .orElseThrow(() -> new EntityNotFoundException("New Office not found with id: " + newOfficeId));

        if (!membership.getMlsRegion().equals(newOffice.getMlsRegion())) {
            throw new IllegalArgumentException("Cannot transfer membership to an office in a different MLS region.");
        }

        membership.setOffice(newOffice);
        return membershipRepository.save(membership);
    }

    /**
     * Get agents by supervising broker.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getAgentsBySupervisingBroker(UUID brokerId) {
        User broker = userRepository.findById(brokerId)
            .orElseThrow(() -> new EntityNotFoundException("Broker not found with id: " + brokerId));
        return membershipRepository.findByOffice_SupervisingBroker(broker);
    }

    /**
     * Validate MLS number.
     */
    @Transactional(readOnly = true)
    public boolean validateMlsNumber(String mlsNumber) {
        return membershipRepository.findByMembershipNumber(mlsNumber).isPresent();
    }

    /**
     * Get memberships by user ID.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsByUserId(UUID userId) {
        return membershipRepository.findByUser_Id(userId);
    }

    /**
     * Get memberships by region.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsByRegion(UUID regionId, Boolean isActive) {
        if (isActive != null) {
            return membershipRepository.findByMlsRegion_IdAndStatus(regionId, isActive ? MLSMembership.MembershipStatus.ACTIVE : MLSMembership.MembershipStatus.INACTIVE);
        }
        return membershipRepository.findByMlsRegion_Id(regionId);
    }

    /**
     * Get memberships by office.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsByOffice(UUID officeId) {
        return membershipRepository.findByOffice_Id(officeId);
    }

    /**
     * Get memberships by type.
     */
    @Transactional(readOnly = true)
    public List<MLSMembership> getMembershipsByType(MLSMembership.MembershipType type, UUID regionId) {
        if (regionId != null) {
            return membershipRepository.findByMlsRegion_IdAndType(regionId, type);
        }
        return membershipRepository.findByType(type);
    }

    /**
     * Search memberships.
     */
    public List<MLSMembership> searchMemberships(Specification<MLSMembership> spec) {
        return membershipRepository.findAll(spec);
    }

    private String generateMlsNumber(MLSRegion region, MLSMembership.MembershipType type) {
        // Example: R1-A-12345
        String prefix = "R" + region.getRegionCode().toUpperCase();
        String typePrefix = type.toString().substring(0, 1);
        // This is a simplification. A robust implementation would handle concurrency.
        long count = membershipRepository.countByMlsRegion_IdAndType(region.getId(), type);
        return String.format("%s-%s-%05d", prefix, typePrefix, count + 1);
    }
} 