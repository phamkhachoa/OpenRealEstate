package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.proptech.realestate.model.entity.ListingCategory;
import com.proptech.realestate.model.entity.User;
import com.proptech.realestate.model.entity.ListingAttribute;

@Entity
@Table(name = "listings")
@Data
public class Listing {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "listing_code", unique = true, nullable = false)
    private String listingCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ListingCategory category;

    @Column(name = "full_address")
    private String fullAddress;

    // Geospatial coordinates  
    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Double area;

    private Integer numBedrooms;
    private Integer numBathrooms;

    @Column(name = "listing_type")
    @Enumerated(EnumType.STRING)
    private ListingType listingType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ListingStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ListingAttribute> attributes = new HashSet<>();

    // MLS-specific fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mls_region_id")
    private MLSRegion mlsRegion;

    @Column(name = "mls_number", unique = true, length = 50)
    private String mlsNumber;

    @Column(name = "original_list_price", precision = 12, scale = 2)
    private BigDecimal originalListPrice;

    // Agent and Office Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_agent_id")
    private User listingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_office_id")
    private Office listingOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selling_agent_id")
    private User sellingAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "selling_office_id")
    private Office sellingOffice;

    // Commission Information
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_structure_id")
    private CommissionStructure commissionStructure;

    @Column(name = "total_commission_percentage", precision = 5, scale = 2)
    private BigDecimal totalCommissionPercentage;

    @Column(name = "listing_commission_percentage", precision = 5, scale = 2)
    private BigDecimal listingCommissionPercentage;

    @Column(name = "selling_commission_percentage", precision = 5, scale = 2)
    private BigDecimal sellingCommissionPercentage;

    // MLS Compliance and Workflow
    @Column(name = "mls_submission_date")
    private LocalDateTime mlsSubmissionDate;

    @Column(name = "last_mls_update")
    private LocalDateTime lastMLSUpdate;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", length = 20)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    // Sharing and Cooperation
    @Column(name = "is_shared_listing", nullable = false)
    private Boolean isSharedListing = true;

    @Column(name = "sharing_restrictions", columnDefinition = "TEXT")
    private String sharingRestrictions;

    @Column(name = "cooperation_compensation", columnDefinition = "TEXT")
    private String cooperationCompensation;

    // Showing Information
    @Column(name = "showing_instructions", columnDefinition = "TEXT")
    private String showingInstructions;

    @Column(name = "lockbox_type", length = 50)
    private String lockboxType;

    @Column(name = "lockbox_location", length = 255)
    private String lockboxLocation;

    @Column(name = "showing_contact_name", length = 255)
    private String showingContactName;

    @Column(name = "showing_contact_phone", length = 20)
    private String showingContactPhone;

    @Column(name = "showing_requirements", columnDefinition = "TEXT")
    private String showingRequirements;

    // Private Remarks (MLS members only)
    @Column(name = "private_remarks", columnDefinition = "TEXT")
    private String privateRemarks;

    @Column(name = "broker_remarks", columnDefinition = "TEXT")
    private String brokerRemarks;

    // Market Information
    @Column(name = "days_on_market", nullable = false)
    private Integer daysOnMarket = 0;

    @Column(name = "cumulative_days_on_market", nullable = false)
    private Integer cumulativeDaysOnMarket = 0;

    @Column(name = "price_change_timestamp")
    private LocalDateTime priceChangeTimestamp;

    // Transaction Information
    @Column(name = "under_contract_date")
    private java.time.LocalDate underContractDate;

    @Column(name = "closing_date")
    private java.time.LocalDate closingDate;

    @Column(name = "sold_price", precision = 12, scale = 2)
    private BigDecimal soldPrice;

    // Enums
    public enum ListingType {
        RENT, SALE, RENTING, BUY, CHANGE
    }

    public enum ListingStatus {
        INACTIVE, ACTIVE, MODERATION, DRAFT, SOLD, EXPIRED, PENDING, UNDER_CONTRACT
    }

    public enum ApprovalStatus {
        PENDING,    // Waiting for approval
        APPROVED,   // Approved and active
        REJECTED,   // Rejected - needs revision
        EXPIRED     // Approval expired
    }

    // Helper methods
    public String getAddress() {
        return this.fullAddress;
    }

    // MLS-specific helper methods
    public boolean isMLSListing() {
        return mlsNumber != null && mlsRegion != null;
    }

    public boolean isApproved() {
        return approvalStatus == ApprovalStatus.APPROVED;
    }

    public boolean needsApproval() {
        return approvalStatus == ApprovalStatus.PENDING;
    }

    public boolean isUnderContract() {
        return status == ListingStatus.UNDER_CONTRACT || underContractDate != null;
    }

    public boolean isSold() {
        return status == ListingStatus.SOLD || soldPrice != null;
    }

    public BigDecimal getCurrentPrice() {
        return price;
    }

    public BigDecimal getOriginalPrice() {
        return originalListPrice != null ? originalListPrice : price;
    }

    public boolean hasPriceChanged() {
        return originalListPrice != null && 
               price.compareTo(originalListPrice) != 0;
    }

    public BigDecimal getPriceReduction() {
        if (originalListPrice == null) return BigDecimal.ZERO;
        return originalListPrice.subtract(price);
    }

    public void updatePrice(BigDecimal newPrice) {
        if (originalListPrice == null) {
            originalListPrice = price;
        }
        price = newPrice;
        priceChangeTimestamp = LocalDateTime.now();
        lastMLSUpdate = LocalDateTime.now();
    }

    public void approve(User approver) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approvedBy = approver;
        this.approvedAt = LocalDateTime.now();
        this.status = ListingStatus.ACTIVE;
    }

    public void reject() {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.status = ListingStatus.DRAFT;
    }

    public void submitToMLS() {
        this.mlsSubmissionDate = LocalDateTime.now();
        this.approvalStatus = ApprovalStatus.PENDING;
        if (mlsRegion != null && !mlsRegion.getRequiresApproval()) {
            this.approvalStatus = ApprovalStatus.APPROVED;
            this.status = ListingStatus.ACTIVE;
        }
    }

    public void markSold(BigDecimal salePrice, java.time.LocalDate closingDate) {
        this.soldPrice = salePrice;
        this.closingDate = closingDate;
        this.status = ListingStatus.SOLD;
        this.lastMLSUpdate = LocalDateTime.now();
    }

    public void markUnderContract(java.time.LocalDate contractDate) {
        this.underContractDate = contractDate;
        this.status = ListingStatus.UNDER_CONTRACT;
        this.lastMLSUpdate = LocalDateTime.now();
    }

    public void expire() {
        this.status = ListingStatus.EXPIRED;
        this.lastMLSUpdate = LocalDateTime.now();
    }

    public void updateDaysOnMarket() {
        if (createdAt != null) {
            this.daysOnMarket = (int) java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
        }
    }
} 