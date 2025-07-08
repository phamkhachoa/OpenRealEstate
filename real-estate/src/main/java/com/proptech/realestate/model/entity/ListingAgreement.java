package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a listing agreement for MLS cooperation.
 * Manages commission sharing and cooperation terms between listing and selling agents.
 */
@Entity
@Table(name = "listing_agreements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ListingAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_agent_id", nullable = false)
    private MLSMembership listingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_office_id", nullable = false)
    private Office listingOffice;

    @Enumerated(EnumType.STRING)
    @Column(name = "agreement_type", nullable = false)
    private AgreementType agreementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "listing_type", nullable = false)
    private ListingType listingType;

    @Column(name = "total_commission_rate", precision = 5, scale = 4)
    private BigDecimal totalCommissionRate; // e.g., 0.06 for 6%

    @Column(name = "total_commission_amount", precision = 15, scale = 2)
    private BigDecimal totalCommissionAmount; // Fixed amount if not percentage

    @Column(name = "selling_commission_rate", precision = 5, scale = 4)
    private BigDecimal sellingCommissionRate; // e.g., 0.03 for 3%

    @Column(name = "selling_commission_amount", precision = 15, scale = 2)
    private BigDecimal sellingCommissionAmount; // Fixed amount for selling side

    @Column(name = "listing_commission_rate", precision = 5, scale = 4)
    private BigDecimal listingCommissionRate; // e.g., 0.03 for 3%

    @Column(name = "listing_commission_amount", precision = 15, scale = 2)
    private BigDecimal listingCommissionAmount; // Fixed amount for listing side

    @Enumerated(EnumType.STRING)
    @Column(name = "commission_structure")
    private CommissionStructureType commissionStructure;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "auto_renewal", nullable = false)
    @Builder.Default
    private Boolean autoRenewal = false;

    @Column(name = "renewal_period_months")
    private Integer renewalPeriodMonths;

    @Enumerated(EnumType.STRING)
    @Column(name = "cooperation_level", nullable = false)
    private CooperationLevel cooperationLevel;

    @Column(name = "allows_subagency", nullable = false)
    @Builder.Default
    private Boolean allowsSubagency = true;

    @Column(name = "allows_buyer_agency", nullable = false)
    @Builder.Default
    private Boolean allowsBuyerAgency = true;

    @Column(name = "allows_dual_agency", nullable = false)
    @Builder.Default
    private Boolean allowsDualAgency = false;

    @Column(name = "requires_showing_instructions", nullable = false)
    @Builder.Default
    private Boolean requiresShowingInstructions = false;

    @Column(name = "compensation_paid_by")
    private String compensationPaidBy; // Seller, Buyer, Split, etc.

    @Column(name = "variable_commission_notes", length = 1000)
    private String variableCommissionNotes;

    @Column(name = "compensation_exceptions", length = 1000)
    private String compensationExceptions;

    @Column(name = "special_terms", columnDefinition = "TEXT")
    private String specialTerms;

    @Column(name = "showing_requirements", length = 500)
    private String showingRequirements;

    @Column(name = "key_box_access", nullable = false)
    @Builder.Default
    private Boolean keyBoxAccess = true;

    @Column(name = "lockbox_number")
    private String lockboxNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.ACTIVE;

    @Column(name = "is_exclusive", nullable = false)
    @Builder.Default
    private Boolean isExclusive = true;

    @Column(name = "syndication_allowed", nullable = false)
    @Builder.Default
    private Boolean syndicationAllowed = true;

    @ElementCollection
    @CollectionTable(name = "agreement_syndication_sites", 
                    joinColumns = @JoinColumn(name = "agreement_id"))
    @Column(name = "site_name")
    @Builder.Default
    private List<String> allowedSyndicationSites = new ArrayList<>();

    @OneToMany(mappedBy = "listingAgreement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CooperatingAgreement> cooperatingAgreements = new ArrayList<>();

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
    public enum AgreementType {
        EXCLUSIVE_RIGHT_TO_SELL,
        EXCLUSIVE_AGENCY,
        OPEN_LISTING,
        NET_LISTING,
        AUCTION_LISTING
    }

    public enum ListingType {
        FOR_SALE,
        FOR_RENT,
        SOLD,
        LEASED,
        WITHDRAWN,
        EXPIRED,
        PENDING
    }

    public enum CommissionStructureType {
        PERCENTAGE,
        FLAT_FEE,
        GRADUATED,
        BONUS,
        VARIABLE,
        PERFORMANCE_BASED
    }

    public enum CooperationLevel {
        FULL_COOPERATION,
        LIMITED_COOPERATION,
        NO_COOPERATION,
        APPROVAL_REQUIRED,
        MLS_MEMBERS_ONLY
    }

    public enum AgreementStatus {
        DRAFT,
        ACTIVE,
        EXPIRED,
        TERMINATED,
        COMPLETED,
        SUSPENDED
    }

    // Business methods
    public boolean isActive() {
        return status == AgreementStatus.ACTIVE && 
               !LocalDate.now().isAfter(endDate);
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(endDate) || 
               status == AgreementStatus.EXPIRED;
    }

    public long getDaysUntilExpiry() {
        if (endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);
    }

    public boolean needsRenewal() {
        return autoRenewal && getDaysUntilExpiry() <= 30;
    }

    public BigDecimal calculateSellingCommission(BigDecimal salePrice) {
        if (salePrice == null) return BigDecimal.ZERO;
        
        if (sellingCommissionAmount != null && sellingCommissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            return sellingCommissionAmount;
        }
        
        if (sellingCommissionRate != null && sellingCommissionRate.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice.multiply(sellingCommissionRate);
        }
        
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateListingCommission(BigDecimal salePrice) {
        if (salePrice == null) return BigDecimal.ZERO;
        
        if (listingCommissionAmount != null && listingCommissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            return listingCommissionAmount;
        }
        
        if (listingCommissionRate != null && listingCommissionRate.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice.multiply(listingCommissionRate);
        }
        
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateTotalCommission(BigDecimal salePrice) {
        if (salePrice == null) return BigDecimal.ZERO;
        
        if (totalCommissionAmount != null && totalCommissionAmount.compareTo(BigDecimal.ZERO) > 0) {
            return totalCommissionAmount;
        }
        
        if (totalCommissionRate != null && totalCommissionRate.compareTo(BigDecimal.ZERO) > 0) {
            return salePrice.multiply(totalCommissionRate);
        }
        
        // Fallback: sum of listing + selling commissions
        return calculateListingCommission(salePrice).add(calculateSellingCommission(salePrice));
    }

    public boolean allowsCooperation() {
        return cooperationLevel != CooperationLevel.NO_COOPERATION && 
               status == AgreementStatus.ACTIVE;
    }

    public boolean allowsSyndication() {
        return syndicationAllowed && status == AgreementStatus.ACTIVE;
    }

    public boolean requiresApprovalForShowing() {
        return requiresShowingInstructions || 
               cooperationLevel == CooperationLevel.APPROVAL_REQUIRED;
    }

    public void addCooperatingAgreement(CooperatingAgreement cooperatingAgreement) {
        if (cooperatingAgreements == null) {
            cooperatingAgreements = new ArrayList<>();
        }
        cooperatingAgreements.add(cooperatingAgreement);
        cooperatingAgreement.setListingAgreement(this);
    }

    public void removeCooperatingAgreement(CooperatingAgreement cooperatingAgreement) {
        if (cooperatingAgreements != null) {
            cooperatingAgreements.remove(cooperatingAgreement);
            cooperatingAgreement.setListingAgreement(null);
        }
    }

    public void extend(int additionalDays) {
        if (endDate != null) {
            endDate = endDate.plusDays(additionalDays);
        }
    }

    public void renew() {
        if (autoRenewal && renewalPeriodMonths != null) {
            startDate = endDate.plusDays(1);
            endDate = startDate.plusMonths(renewalPeriodMonths);
            status = AgreementStatus.ACTIVE;
        }
    }

    public void terminate() {
        status = AgreementStatus.TERMINATED;
        endDate = LocalDate.now();
    }

    public void expire() {
        status = AgreementStatus.EXPIRED;
    }

    public void complete() {
        status = AgreementStatus.COMPLETED;
    }
} 