package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents MLS membership for agents and brokers.
 * Defines access permissions and membership status within an MLS region.
 */
@Entity
@Table(name = "mls_memberships")
@Data
@EqualsAndHashCode(callSuper = false)
public class MLSMembership {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Agent or Broker

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mls_region_id", nullable = false)
    private MLSRegion mlsRegion;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_type", nullable = false)
    private MembershipType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_status", nullable = false)
    private MembershipStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "annual_fee", precision = 10, scale = 2)
    private BigDecimal annualFee;

    @Column(name = "membership_number", unique = true, length = 50)
    private String membershipNumber; // AUTO-GENERATED: SEA2024AG001234

    // Office association (for agents)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    // License Information
    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_state", length = 2)
    private String licenseState;

    @Column(name = "license_expiry_date")
    private LocalDate licenseExpiryDate;

    // Permissions
    @Column(name = "can_list_properties", nullable = false)
    private Boolean canListProperties = false;

    @Column(name = "can_view_all_listings", nullable = false)
    private Boolean canViewAllListings = false;

    @Column(name = "can_access_idx_feeds", nullable = false)
    private Boolean canAccessIDXFeeds = false;

    @Column(name = "can_manage_office", nullable = false)
    private Boolean canManageOffice = false;

    @Column(name = "can_view_showing_info", nullable = false)
    private Boolean canViewShowingInfo = false;

    @Column(name = "can_view_agent_contact", nullable = false)
    private Boolean canViewAgentContact = false;

    @Column(name = "can_export_data", nullable = false)
    private Boolean canExportData = false;

    @Column(name = "can_bulk_import", nullable = false)
    private Boolean canBulkImport = false;

    // Administrative permissions
    @Column(name = "can_approve_listings", nullable = false)
    private Boolean canApproveListings = false;

    @Column(name = "can_manage_members", nullable = false)
    private Boolean canManageMembers = false;

    @Column(name = "can_configure_rules", nullable = false)
    private Boolean canConfigureRules = false;

    @Column(name = "can_access_analytics", nullable = false)
    private Boolean canAccessAnalytics = false;

    @Column(name = "can_manage_commissions", nullable = false)
    private Boolean canManageCommissions = false;

    // Compliance tracking
    @Column(name = "training_completed", nullable = false)
    private Boolean trainingCompleted = false;

    @Column(name = "training_completion_date")
    private LocalDate trainingCompletionDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "agreement_accepted", nullable = false)
    private Boolean agreementAccepted = false;

    @Column(name = "agreement_accepted_date")
    private LocalDateTime agreementAcceptedDate;

    // Billing
    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    @Column(name = "next_payment_due")
    private LocalDate nextPaymentDue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum MembershipType {
        SUPER_ADMIN,     // System administration
        MLS_ADMIN,       // Region administration  
        BROKER_ADMIN,    // Office management
        BROKER,          // Licensed broker
        AGENT,           // Licensed agent
        ASSISTANT,       // Unlicensed assistant
        READONLY_MEMBER, // View-only access
        IDX_CONSUMER     // External website access
    }

    public enum MembershipStatus {
        PENDING,         // Application submitted
        ACTIVE,          // Full access
        SUSPENDED,       // Temporarily disabled
        EXPIRED,         // Needs renewal
        TERMINATED,      // Permanently removed
        GRACE_PERIOD     // Past due but still active
    }

    // Business methods
    public boolean isActive() {
        return status == MembershipStatus.ACTIVE || status == MembershipStatus.GRACE_PERIOD;
    }

    public boolean isExpired() {
        return status == MembershipStatus.EXPIRED || 
               (endDate != null && endDate.isBefore(LocalDate.now()));
    }

    public boolean hasAdminRights() {
        return type == MembershipType.SUPER_ADMIN || 
               type == MembershipType.MLS_ADMIN || 
               type == MembershipType.BROKER_ADMIN;
    }

    public boolean canAccessMLS() {
        return isActive() && trainingCompleted && agreementAccepted;
    }

    public String generateMembershipNumber() {
        String prefix = mlsRegion != null ? mlsRegion.getListingPrefix() : "MLS";
        String typeCode = switch(type) {
            case SUPER_ADMIN -> "SA";
            case MLS_ADMIN -> "MA"; 
            case BROKER_ADMIN -> "BA";
            case BROKER -> "BR";
            case AGENT -> "AG";
            case ASSISTANT -> "AS";
            case READONLY_MEMBER -> "RO";
            case IDX_CONSUMER -> "IDX";
        };
        
        return String.format("%s%d%s%06d", 
            prefix,
            java.time.Year.now().getValue(),
            typeCode,
            System.currentTimeMillis() % 1000000);
    }

    public void setDefaultPermissions() {
        switch(type) {
            case SUPER_ADMIN:
                setAllPermissions(true);
                break;
            case MLS_ADMIN:
                setAdminPermissions();
                break;
            case BROKER_ADMIN:
                setBrokerPermissions();
                break;
            case BROKER:
            case AGENT:
                setAgentPermissions();
                break;
            case ASSISTANT:
                setAssistantPermissions();
                break;
            case READONLY_MEMBER:
                setReadOnlyPermissions();
                break;
            case IDX_CONSUMER:
                setIDXPermissions();
                break;
        }
    }

    private void setAllPermissions(boolean value) {
        canListProperties = value;
        canViewAllListings = value;
        canAccessIDXFeeds = value;
        canManageOffice = value;
        canViewShowingInfo = value;
        canViewAgentContact = value;
        canExportData = value;
        canBulkImport = value;
        canApproveListings = value;
        canManageMembers = value;
        canConfigureRules = value;
        canAccessAnalytics = value;
        canManageCommissions = value;
    }

    private void setAdminPermissions() {
        canListProperties = true;
        canViewAllListings = true;
        canAccessIDXFeeds = true;
        canManageOffice = true;
        canViewShowingInfo = true;
        canViewAgentContact = true;
        canExportData = true;
        canBulkImport = true;
        canApproveListings = true;
        canManageMembers = true;
        canConfigureRules = true;
        canAccessAnalytics = true;
        canManageCommissions = true;
    }

    private void setBrokerPermissions() {
        canListProperties = true;
        canViewAllListings = true;
        canAccessIDXFeeds = true;
        canManageOffice = true;
        canViewShowingInfo = true;
        canViewAgentContact = true;
        canExportData = true;
        canBulkImport = true;
        canApproveListings = true;
        canAccessAnalytics = true;
        canManageCommissions = true;
    }

    private void setAgentPermissions() {
        canListProperties = true;
        canViewAllListings = true;
        canAccessIDXFeeds = true;
        canViewShowingInfo = true;
        canViewAgentContact = true;
        canExportData = false;
        canAccessAnalytics = false;
    }

    private void setAssistantPermissions() {
        canListProperties = false;
        canViewAllListings = true;
        canAccessIDXFeeds = false;
        canViewShowingInfo = true;
        canViewAgentContact = false;
    }

    private void setReadOnlyPermissions() {
        canViewAllListings = true;
        canViewShowingInfo = false;
        canViewAgentContact = false;
    }

    private void setIDXPermissions() {
        canViewAllListings = true;
        canAccessIDXFeeds = true;
    }
} 