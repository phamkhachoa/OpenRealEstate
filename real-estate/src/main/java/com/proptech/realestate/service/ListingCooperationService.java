package com.proptech.realestate.service;

import com.proptech.realestate.model.entity.*;
import com.proptech.realestate.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing MLS listing cooperation and commission sharing.
 * Handles the business logic for agent collaboration, showings, and co-broke agreements.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ListingCooperationService {

    private final ListingAgreementRepository listingAgreementRepository;
    private final ListingRepository listingRepository;
    private final MLSMembershipRepository membershipRepository;
    private final OfficeRepository officeRepository;
    private final UserRepository userRepository;

    /**
     * Create a listing agreement for MLS cooperation.
     */
    public ListingAgreement createListingAgreement(CreateListingAgreementRequest request) {
        log.info("Creating listing agreement for listing: {}", request.getListingId());
        
        // Validate listing exists
        Listing listing = listingRepository.findById(request.getListingId())
            .orElseThrow(() -> new EntityNotFoundException("Listing not found: " + request.getListingId()));
        
        // Validate listing agent
        MLSMembership listingAgent = membershipRepository.findById(request.getListingAgentId())
            .orElseThrow(() -> new EntityNotFoundException("Listing agent not found: " + request.getListingAgentId()));
        
        // Validate listing office
        Office listingOffice = officeRepository.findById(request.getListingOfficeId())
            .orElseThrow(() -> new EntityNotFoundException("Listing office not found: " + request.getListingOfficeId()));
        
        // Check if agreement already exists
        if (listingAgreementRepository.hasActiveAgreement(request.getListingId())) {
            throw new IllegalStateException("Active listing agreement already exists for this listing");
        }
        
        // Validate commission rates
        validateCommissionRates(request.getTotalCommissionRate(), request.getSellingCommissionRate(), 
                              request.getListingCommissionRate());
        
        // Create listing agreement
        ListingAgreement agreement = ListingAgreement.builder()
            .listing(listing)
            .listingAgent(listingAgent)
            .listingOffice(listingOffice)
            .agreementType(request.getAgreementType())
            .listingType(request.getListingType())
            .totalCommissionRate(request.getTotalCommissionRate())
            .totalCommissionAmount(request.getTotalCommissionAmount())
            .sellingCommissionRate(request.getSellingCommissionRate())
            .sellingCommissionAmount(request.getSellingCommissionAmount())
            .listingCommissionRate(request.getListingCommissionRate())
            .listingCommissionAmount(request.getListingCommissionAmount())
            .commissionStructure(request.getCommissionStructure())
            .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
            .endDate(request.getEndDate())
            .autoRenewal(request.getAutoRenewal() != null ? request.getAutoRenewal() : false)
            .renewalPeriodMonths(request.getRenewalPeriodMonths())
            .cooperationLevel(request.getCooperationLevel())
            .allowsSubagency(request.getAllowsSubagency() != null ? request.getAllowsSubagency() : true)
            .allowsBuyerAgency(request.getAllowsBuyerAgency() != null ? request.getAllowsBuyerAgency() : true)
            .allowsDualAgency(request.getAllowsDualAgency() != null ? request.getAllowsDualAgency() : false)
            .requiresShowingInstructions(request.getRequiresShowingInstructions() != null ? 
                                       request.getRequiresShowingInstructions() : false)
            .compensationPaidBy(request.getCompensationPaidBy())
            .variableCommissionNotes(request.getVariableCommissionNotes())
            .compensationExceptions(request.getCompensationExceptions())
            .specialTerms(request.getSpecialTerms())
            .showingRequirements(request.getShowingRequirements())
            .keyBoxAccess(request.getKeyBoxAccess() != null ? request.getKeyBoxAccess() : true)
            .lockboxNumber(request.getLockboxNumber())
            .isExclusive(request.getIsExclusive() != null ? request.getIsExclusive() : true)
            .syndicationAllowed(request.getSyndicationAllowed() != null ? request.getSyndicationAllowed() : true)
            .allowedSyndicationSites(request.getAllowedSyndicationSites())
            .createdBy(request.getCreatedBy())
            .build();
        
        ListingAgreement savedAgreement = listingAgreementRepository.save(agreement);
        log.info("Created listing agreement with ID: {}", savedAgreement.getId());
        
        return savedAgreement;
    }

    /**
     * Create a cooperating agreement between listing and selling agents.
     */
    public CooperatingAgreement createCooperatingAgreement(CreateCooperatingAgreementRequest request) {
        log.info("Creating cooperating agreement for listing agreement: {}", request.getListingAgreementId());
        
        // Validate listing agreement
        ListingAgreement listingAgreement = listingAgreementRepository.findById(request.getListingAgreementId())
            .orElseThrow(() -> new EntityNotFoundException("Listing agreement not found: " + request.getListingAgreementId()));
        
        // Validate selling agent
        MLSMembership sellingAgent = membershipRepository.findById(request.getSellingAgentId())
            .orElseThrow(() -> new EntityNotFoundException("Selling agent not found: " + request.getSellingAgentId()));
        
        // Validate selling office
        Office sellingOffice = officeRepository.findById(request.getSellingOfficeId())
            .orElseThrow(() -> new EntityNotFoundException("Selling office not found: " + request.getSellingOfficeId()));
        
        // Validate cooperation is allowed
        if (!listingAgreement.allowsCooperation()) {
            throw new IllegalStateException("Cooperation is not allowed for this listing agreement");
        }
        
        // Validate buyer if provided
        User buyer = null;
        if (request.getBuyerId() != null) {
            buyer = userRepository.findById(request.getBuyerId())
                .orElseThrow(() -> new EntityNotFoundException("Buyer not found: " + request.getBuyerId()));
        }
        
        // Create cooperating agreement
        CooperatingAgreement cooperatingAgreement = CooperatingAgreement.builder()
            .listingAgreement(listingAgreement)
            .sellingAgent(sellingAgent)
            .sellingOffice(sellingOffice)
            .buyer(buyer)
            .cooperationType(request.getCooperationType())
            .agencyRelationship(request.getAgencyRelationship())
            .agreedCommissionRate(request.getAgreedCommissionRate())
            .agreedCommissionAmount(request.getAgreedCommissionAmount())
            .bonusCommission(request.getBonusCommission())
            .commissionSplitPercentage(request.getCommissionSplitPercentage())
            .offerAmount(request.getOfferAmount())
            .earnestMoney(request.getEarnestMoney())
            .financingContingency(request.getFinancingContingency() != null ? request.getFinancingContingency() : true)
            .inspectionContingency(request.getInspectionContingency() != null ? request.getInspectionContingency() : true)
            .appraisalContingency(request.getAppraisalContingency() != null ? request.getAppraisalContingency() : true)
            .contingencyDeadline(request.getContingencyDeadline())
            .daysToClose(request.getDaysToClose())
            .showingInstructions(request.getShowingInstructions())
            .specialTerms(request.getSpecialTerms())
            .commissionNotes(request.getCommissionNotes())
            .cooperationNotes(request.getCooperationNotes())
            .referralFee(request.getReferralFee())
            .referralNotes(request.getReferralNotes())
            .createdBy(request.getCreatedBy())
            .build();
        
        // Add to listing agreement
        listingAgreement.addCooperatingAgreement(cooperatingAgreement);
        
        CooperatingAgreement savedAgreement = listingAgreementRepository.save(listingAgreement)
            .getCooperatingAgreements()
            .stream()
            .filter(ca -> ca.getId().equals(cooperatingAgreement.getId()))
            .findFirst()
            .orElse(cooperatingAgreement);
        
        log.info("Created cooperating agreement with ID: {}", savedAgreement.getId());
        
        return savedAgreement;
    }

    /**
     * Approve a cooperating agreement.
     */
    public CooperatingAgreement approveCooperatingAgreement(UUID cooperatingAgreementId, UUID approvedByUserId) {
        log.info("Approving cooperating agreement: {} by user: {}", cooperatingAgreementId, approvedByUserId);
        
        CooperatingAgreement agreement = getCooperatingAgreementById(cooperatingAgreementId);
        
        // Validate user has authority to approve
        validateApprovalAuthority(agreement.getListingAgreement(), approvedByUserId);
        
        agreement.approve(approvedByUserId);
        
        return saveCooperatingAgreement(agreement);
    }

    /**
     * Reject a cooperating agreement.
     */
    public CooperatingAgreement rejectCooperatingAgreement(UUID cooperatingAgreementId, String reason) {
        log.info("Rejecting cooperating agreement: {} with reason: {}", cooperatingAgreementId, reason);
        
        CooperatingAgreement agreement = getCooperatingAgreementById(cooperatingAgreementId);
        agreement.reject(reason);
        
        return saveCooperatingAgreement(agreement);
    }

    /**
     * Put cooperating agreement under contract.
     */
    public CooperatingAgreement putUnderContract(UUID cooperatingAgreementId, BigDecimal contractAmount) {
        log.info("Putting cooperating agreement under contract: {} for amount: {}", 
                cooperatingAgreementId, contractAmount);
        
        CooperatingAgreement agreement = getCooperatingAgreementById(cooperatingAgreementId);
        
        if (!agreement.isActive()) {
            throw new IllegalStateException("Cooperating agreement must be active to put under contract");
        }
        
        agreement.putUnderContract(contractAmount);
        
        return saveCooperatingAgreement(agreement);
    }

    /**
     * Close a cooperating agreement and calculate final commissions.
     */
    public CooperatingAgreement closeCooperatingAgreement(UUID cooperatingAgreementId, BigDecimal finalAmount) {
        log.info("Closing cooperating agreement: {} with final amount: {}", cooperatingAgreementId, finalAmount);
        
        CooperatingAgreement agreement = getCooperatingAgreementById(cooperatingAgreementId);
        
        if (agreement.getStatus() != CooperatingAgreement.CooperationStatus.UNDER_CONTRACT) {
            throw new IllegalStateException("Cooperating agreement must be under contract to close");
        }
        
        agreement.close(finalAmount);
        
        // Update listing agreement status if needed
        ListingAgreement listingAgreement = agreement.getListingAgreement();
        if (listingAgreement.getListingType() == ListingAgreement.ListingType.FOR_SALE) {
            listingAgreement.setListingType(ListingAgreement.ListingType.SOLD);
            listingAgreement.complete();
            listingAgreementRepository.save(listingAgreement);
        }
        
        return saveCooperatingAgreement(agreement);
    }

    /**
     * Calculate commission breakdown for a transaction.
     */
    @Transactional(readOnly = true)
    public CommissionBreakdown calculateCommissionBreakdown(UUID cooperatingAgreementId) {
        log.info("Calculating commission breakdown for cooperating agreement: {}", cooperatingAgreementId);
        
        CooperatingAgreement agreement = getCooperatingAgreementById(cooperatingAgreementId);
        
        if (agreement.getClosingAmount() == null) {
            throw new IllegalStateException("Cannot calculate commission breakdown without closing amount");
        }
        
        BigDecimal totalCommission = agreement.getListingAgreement().calculateTotalCommission(agreement.getClosingAmount());
        BigDecimal sellingCommission = agreement.calculateSellingCommission();
        BigDecimal listingCommission = agreement.getListingAgreement().calculateListingCommission(agreement.getClosingAmount());
        BigDecimal agentCommission = agreement.calculateAgentCommission();
        BigDecimal officeCommission = agreement.calculateOfficeCommission();
        
        return CommissionBreakdown.builder()
            .cooperatingAgreementId(cooperatingAgreementId)
            .closingAmount(agreement.getClosingAmount())
            .totalCommission(totalCommission)
            .sellingCommission(sellingCommission)
            .listingCommission(listingCommission)
            .sellingAgentCommission(agentCommission)
            .sellingOfficeCommission(officeCommission)
            .bonusCommission(agreement.getBonusCommission())
            .referralFee(agreement.getReferralFee())
            .commissionSummary(agreement.getCommissionSummary())
            .build();
    }

    /**
     * Get cooperating agreements requiring approval.
     */
    @Transactional(readOnly = true)
    public List<CooperatingAgreement> getPendingApprovals(UUID listingAgentId, UUID officeId) {
        log.info("Getting pending approvals for agent: {} and office: {}", listingAgentId, officeId);
        
        return listingAgreementRepository.findAll()
            .stream()
            .filter(la -> {
                if (listingAgentId != null && !la.getListingAgent().getId().equals(listingAgentId)) {
                    return false;
                }
                if (officeId != null && !la.getListingOffice().getId().equals(officeId)) {
                    return false;
                }
                return true;
            })
            .flatMap(la -> la.getCooperatingAgreements().stream())
            .filter(ca -> ca.getStatus() == CooperatingAgreement.CooperationStatus.PENDING)
            .toList();
    }

    /**
     * Get cooperation statistics.
     */
    @Transactional(readOnly = true)
    public CooperationStats getCooperationStats(UUID regionId, UUID officeId) {
        log.info("Getting cooperation statistics for region: {} and office: {}", regionId, officeId);
        
        List<ListingAgreement> agreements = listingAgreementRepository.findActiveAgreements(LocalDate.now());
        
        if (officeId != null) {
            Office office = officeRepository.findById(officeId)
                .orElseThrow(() -> new EntityNotFoundException("Office not found: " + officeId));
            agreements = agreements.stream()
                .filter(la -> la.getListingOffice().equals(office))
                .toList();
        }
        
        long totalListings = agreements.size();
        long cooperativeListings = agreements.stream()
            .filter(ListingAgreement::allowsCooperation)
            .count();
        long exclusiveListings = agreements.stream()
            .filter(ListingAgreement::getIsExclusive)
            .count();
        long syndicationAllowed = agreements.stream()
            .filter(ListingAgreement::allowsSyndication)
            .count();
        
        long totalCooperatingAgreements = agreements.stream()
            .mapToLong(la -> la.getCooperatingAgreements().size())
            .sum();
        long pendingApprovals = agreements.stream()
            .flatMap(la -> la.getCooperatingAgreements().stream())
            .filter(ca -> ca.getStatus() == CooperatingAgreement.CooperationStatus.PENDING)
            .count();
        long activeCooperations = agreements.stream()
            .flatMap(la -> la.getCooperatingAgreements().stream())
            .filter(CooperatingAgreement::isActive)
            .count();
        long closedTransactions = agreements.stream()
            .flatMap(la -> la.getCooperatingAgreements().stream())
            .filter(CooperatingAgreement::isCompleted)
            .count();
        
        return CooperationStats.builder()
            .regionId(regionId)
            .officeId(officeId)
            .totalListings(totalListings)
            .cooperativeListings(cooperativeListings)
            .exclusiveListings(exclusiveListings)
            .syndicationAllowed(syndicationAllowed)
            .totalCooperatingAgreements(totalCooperatingAgreements)
            .pendingApprovals(pendingApprovals)
            .activeCooperations(activeCooperations)
            .closedTransactions(closedTransactions)
            .cooperationRate(totalListings > 0 ? (double) cooperativeListings / totalListings : 0.0)
            .build();
    }

    // Private helper methods
    private CooperatingAgreement getCooperatingAgreementById(UUID id) {
        return listingAgreementRepository.findAll()
            .stream()
            .flatMap(la -> la.getCooperatingAgreements().stream())
            .filter(ca -> ca.getId().equals(id))
            .findFirst()
            .orElseThrow(() -> new EntityNotFoundException("Cooperating agreement not found: " + id));
    }

    private CooperatingAgreement saveCooperatingAgreement(CooperatingAgreement agreement) {
        ListingAgreement listingAgreement = agreement.getListingAgreement();
        listingAgreementRepository.save(listingAgreement);
        return agreement;
    }

    private void validateCommissionRates(BigDecimal total, BigDecimal selling, BigDecimal listing) {
        if (total != null && selling != null && listing != null) {
            BigDecimal sum = selling.add(listing);
            if (total.compareTo(sum) != 0) {
                throw new IllegalArgumentException("Total commission rate must equal sum of selling and listing rates");
            }
        }
    }

    private void validateApprovalAuthority(ListingAgreement listingAgreement, UUID userId) {
        // Validate user has authority - could be listing agent, broker, or office admin
        // This is a simplified validation - in real implementation, check roles and permissions
        if (!listingAgreement.getListingAgent().getUser().getId().equals(userId) &&
            !listingAgreement.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("User does not have authority to approve this cooperation");
        }
    }

    // DTOs
    @lombok.Data
    @lombok.Builder
    public static class CreateListingAgreementRequest {
        private UUID listingId;
        private UUID listingAgentId;
        private UUID listingOfficeId;
        private ListingAgreement.AgreementType agreementType;
        private ListingAgreement.ListingType listingType;
        private BigDecimal totalCommissionRate;
        private BigDecimal totalCommissionAmount;
        private BigDecimal sellingCommissionRate;
        private BigDecimal sellingCommissionAmount;
        private BigDecimal listingCommissionRate;
        private BigDecimal listingCommissionAmount;
        private ListingAgreement.CommissionStructureType commissionStructure;
        private LocalDate startDate;
        private LocalDate endDate;
        private Boolean autoRenewal;
        private Integer renewalPeriodMonths;
        private ListingAgreement.CooperationLevel cooperationLevel;
        private Boolean allowsSubagency;
        private Boolean allowsBuyerAgency;
        private Boolean allowsDualAgency;
        private Boolean requiresShowingInstructions;
        private String compensationPaidBy;
        private String variableCommissionNotes;
        private String compensationExceptions;
        private String specialTerms;
        private String showingRequirements;
        private Boolean keyBoxAccess;
        private String lockboxNumber;
        private Boolean isExclusive;
        private Boolean syndicationAllowed;
        private List<String> allowedSyndicationSites;
        private UUID createdBy;
    }

    @lombok.Data
    @lombok.Builder
    public static class CreateCooperatingAgreementRequest {
        private UUID listingAgreementId;
        private UUID sellingAgentId;
        private UUID sellingOfficeId;
        private UUID buyerId;
        private CooperatingAgreement.CooperationType cooperationType;
        private CooperatingAgreement.AgencyRelationship agencyRelationship;
        private BigDecimal agreedCommissionRate;
        private BigDecimal agreedCommissionAmount;
        private BigDecimal bonusCommission;
        private BigDecimal commissionSplitPercentage;
        private BigDecimal offerAmount;
        private BigDecimal earnestMoney;
        private Boolean financingContingency;
        private Boolean inspectionContingency;
        private Boolean appraisalContingency;
        private LocalDateTime contingencyDeadline;
        private Integer daysToClose;
        private String showingInstructions;
        private String specialTerms;
        private String commissionNotes;
        private String cooperationNotes;
        private BigDecimal referralFee;
        private String referralNotes;
        private UUID createdBy;
    }

    @lombok.Data
    @lombok.Builder
    public static class CommissionBreakdown {
        private UUID cooperatingAgreementId;
        private BigDecimal closingAmount;
        private BigDecimal totalCommission;
        private BigDecimal sellingCommission;
        private BigDecimal listingCommission;
        private BigDecimal sellingAgentCommission;
        private BigDecimal sellingOfficeCommission;
        private BigDecimal bonusCommission;
        private BigDecimal referralFee;
        private String commissionSummary;
    }

    @lombok.Data
    @lombok.Builder
    public static class CooperationStats {
        private UUID regionId;
        private UUID officeId;
        private Long totalListings;
        private Long cooperativeListings;
        private Long exclusiveListings;
        private Long syndicationAllowed;
        private Long totalCooperatingAgreements;
        private Long pendingApprovals;
        private Long activeCooperations;
        private Long closedTransactions;
        private Double cooperationRate;
    }
} 