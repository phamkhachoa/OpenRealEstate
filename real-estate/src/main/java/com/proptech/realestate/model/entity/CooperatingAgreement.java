package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a cooperating agreement between listing and selling agents.
 * Manages the cooperation terms and commission sharing for specific transactions.
 */
@Entity
@Table(name = "cooperating_agreements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CooperatingAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_agreement_id", nullable = false)
    private ListingAgreement listingAgreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selling_agent_id", nullable = false)
    private MLSMembership sellingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selling_office_id", nullable = false)
    private Office sellingOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private User buyer; // The buyer represented by selling agent

    @Enumerated(EnumType.STRING)
    @Column(name = "cooperation_type", nullable = false)
    private CooperationType cooperationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "agency_relationship", nullable = false)
    private AgencyRelationship agencyRelationship;

    @Column(name = "agreed_commission_rate", precision = 5, scale = 4)
    private BigDecimal agreedCommissionRate;

    @Column(name = "agreed_commission_amount", precision = 15, scale = 2)
    private BigDecimal agreedCommissionAmount;

    @Column(name = "bonus_commission", precision = 15, scale = 2)
    private BigDecimal bonusCommission;

    @Column(name = "commission_split_percentage", precision = 5, scale = 4)
    private BigDecimal commissionSplitPercentage; // Between selling agent and office

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CooperationStatus status = CooperationStatus.PENDING;

    @Column(name = "offer_amount", precision = 15, scale = 2)
    private BigDecimal offerAmount;

    @Column(name = "closing_amount", precision = 15, scale = 2)
    private BigDecimal closingAmount;

    @Column(name = "closing_date")
    private LocalDateTime closingDate;

    @Column(name = "earnest_money", precision = 15, scale = 2)
    private BigDecimal earnestMoney;

    @Column(name = "financing_contingency", nullable = false)
    @Builder.Default
    private Boolean financingContingency = true;

    @Column(name = "inspection_contingency", nullable = false)
    @Builder.Default
    private Boolean inspectionContingency = true;

    @Column(name = "appraisal_contingency", nullable = false)
    @Builder.Default
    private Boolean appraisalContingency = true;

    @Column(name = "contingency_deadline")
    private LocalDateTime contingencyDeadline;

    @Column(name = "days_to_close")
    private Integer daysToClose;

    @Column(name = "showing_instructions", length = 1000)
    private String showingInstructions;

    @Column(name = "special_terms", columnDefinition = "TEXT")
    private String specialTerms;

    @Column(name = "commission_notes", length = 1000)
    private String commissionNotes;

    @Column(name = "cooperation_notes", length = 1000)
    private String cooperationNotes;

    @Column(name = "referral_fee", precision = 15, scale = 2)
    private BigDecimal referralFee;

    @Column(name = "referral_notes", length = 500)
    private String referralNotes;

    @Column(name = "approved_by_listing_agent", nullable = false)
    @Builder.Default
    private Boolean approvedByListingAgent = false;

    @Column(name = "approved_by_listing_office", nullable = false)
    @Builder.Default
    private Boolean approvedByListingOffice = false;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approved_by_user_id")
    private UUID approvedByUserId;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "modification_requested", nullable = false)
    @Builder.Default
    private Boolean modificationRequested = false;

    @Column(name = "modification_notes", length = 1000)
    private String modificationNotes;

    @OneToMany(mappedBy = "cooperatingAgreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ShowingRequest> showingRequests = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    // Enums
    public enum CooperationType {
        BUYER_REPRESENTATION,
        SUBAGENCY,
        DUAL_AGENCY,
        TRANSACTION_BROKER,
        FACILITATOR,
        REFERRAL_ONLY
    }

    public enum AgencyRelationship {
        BUYERS_AGENT,
        SUBAGENT,
        DUAL_AGENT,
        TRANSACTION_BROKER,
        FACILITATOR,
        DISCLOSED_DUAL_AGENT
    }

    public enum CooperationStatus {
        PENDING,
        APPROVED,
        REJECTED,
        ACTIVE,
        COMPLETED,
        TERMINATED,
        EXPIRED,
        UNDER_CONTRACT,
        CLOSED,
        CANCELLED
    }

    // Business methods
    public boolean isActive() {
        return status == CooperationStatus.ACTIVE || 
               status == CooperationStatus.APPROVED;
    }

    public boolean isPending() {
        return status == CooperationStatus.PENDING;
    }

    public boolean isCompleted() {
        return status == CooperationStatus.COMPLETED || 
               status == CooperationStatus.CLOSED;
    }

    public boolean requiresApproval() {
        return !approvedByListingAgent || !approvedByListingOffice;
    }

    public BigDecimal calculateSellingCommission() {
        if (closingAmount == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal commission = BigDecimal.ZERO;
        
        if (agreedCommissionAmount != null && agreedCommissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            commission = agreedCommissionAmount;
        } else if (agreedCommissionRate != null && agreedCommissionRate.compareTo(BigDecimal.ZERO) > 0) {
            commission = closingAmount.multiply(agreedCommissionRate);
        } else {
            // Fallback to listing agreement selling commission
            commission = listingAgreement.calculateSellingCommission(closingAmount);
        }

        // Add bonus commission if any
        if (bonusCommission != null && bonusCommission.compareTo(BigDecimal.ZERO) > 0) {
            commission = commission.add(bonusCommission);
        }

        return commission;
    }

    public BigDecimal calculateAgentCommission() {
        BigDecimal totalCommission = calculateSellingCommission();
        
        if (commissionSplitPercentage != null && commissionSplitPercentage.compareTo(BigDecimal.ZERO) > 0) {
            return totalCommission.multiply(commissionSplitPercentage);
        }
        
        // Default to 100% if no split specified
        return totalCommission;
    }

    public BigDecimal calculateOfficeCommission() {
        BigDecimal totalCommission = calculateSellingCommission();
        BigDecimal agentCommission = calculateAgentCommission();
        
        return totalCommission.subtract(agentCommission);
    }

    public boolean hasContingencies() {
        return financingContingency || inspectionContingency || appraisalContingency;
    }

    public boolean isContingencyExpired() {
        return contingencyDeadline != null && 
               LocalDateTime.now().isAfter(contingencyDeadline);
    }

    public long getDaysUntilClosing() {
        if (closingDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(
            LocalDateTime.now().toLocalDate(), 
            closingDate.toLocalDate()
        );
    }

    public void approve(UUID approvedByUserId) {
        this.status = CooperationStatus.APPROVED;
        this.approvedByListingAgent = true;
        this.approvedByListingOffice = true;
        this.approvalDate = LocalDateTime.now();
        this.approvedByUserId = approvedByUserId;
        this.modificationRequested = false;
    }

    public void reject(String reason) {
        this.status = CooperationStatus.REJECTED;
        this.rejectionReason = reason;
        this.approvedByListingAgent = false;
        this.approvedByListingOffice = false;
    }

    public void requestModification(String notes) {
        this.modificationRequested = true;
        this.modificationNotes = notes;
        this.status = CooperationStatus.PENDING;
    }

    public void putUnderContract(BigDecimal contractAmount) {
        this.status = CooperationStatus.UNDER_CONTRACT;
        this.offerAmount = contractAmount;
    }

    public void close(BigDecimal finalAmount) {
        this.status = CooperationStatus.CLOSED;
        this.closingAmount = finalAmount;
        this.closingDate = LocalDateTime.now();
    }

    public void terminate(String reason) {
        this.status = CooperationStatus.TERMINATED;
        this.rejectionReason = reason;
    }

    public void cancel() {
        this.status = CooperationStatus.CANCELLED;
    }

    public void addShowingRequest(ShowingRequest showingRequest) {
        if (showingRequests == null) {
            showingRequests = new ArrayList<>();
        }
        showingRequests.add(showingRequest);
        showingRequest.setCooperatingAgreement(this);
    }

    public void removeShowingRequest(ShowingRequest showingRequest) {
        if (showingRequests != null) {
            showingRequests.remove(showingRequest);
            showingRequest.setCooperatingAgreement(null);
        }
    }

    public boolean allowsShowing() {
        return isActive() && listingAgreement.allowsCooperation();
    }

    public boolean requiresShowingApproval() {
        return listingAgreement.requiresApprovalForShowing();
    }

    public String getCommissionSummary() {
        StringBuilder summary = new StringBuilder();
        
        if (agreedCommissionRate != null) {
            summary.append(String.format("%.2f%%", agreedCommissionRate.multiply(new BigDecimal(100))));
        } else if (agreedCommissionAmount != null) {
            summary.append(String.format("$%.2f", agreedCommissionAmount));
        }
        
        if (bonusCommission != null && bonusCommission.compareTo(BigDecimal.ZERO) > 0) {
            summary.append(String.format(" + $%.2f bonus", bonusCommission));
        }
        
        return summary.toString();
    }
} 