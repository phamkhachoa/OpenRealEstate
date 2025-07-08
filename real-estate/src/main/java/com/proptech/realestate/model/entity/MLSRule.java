package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents region-specific business rules and compliance requirements within an MLS.
 * Stores rule parameters as JSON for flexible rule definitions.
 */
@Entity
@Table(name = "mls_rules")
@Data
@EqualsAndHashCode(callSuper = false)
public class MLSRule {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mls_region_id", nullable = false)
    private MLSRegion mlsRegion;

    @Column(name = "rule_name", nullable = false, length = 255)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    private RuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_category", length = 50)
    private RuleCategory ruleCategory;

    @Lob
    private String description;

    // Store rule parameters as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rule_value", columnDefinition = "jsonb")
    private String ruleValue;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum RuleType {
        LISTING_VALIDATION,     // Rules for listing data validation
        COMMISSION_POLICY,      // Commission-related rules
        SHOWING_REQUIREMENT,    // Property showing requirements
        DATA_SHARING,          // Data sharing and access rules
        WORKFLOW_AUTOMATION,   // Automated workflow rules
        COMPLIANCE_CHECK,      // Compliance checking rules
        MEMBERSHIP_REQUIREMENT // Membership validation rules
    }

    public enum RuleCategory {
        MANDATORY,    // Must be enforced
        OPTIONAL,     // Can be overridden
        RECOMMENDED,  // Suggested best practice
        WARNING       // Shows warning but allows
    }

    // Business methods
    public boolean isEffective() {
        LocalDate now = LocalDate.now();
        
        if (!isActive) {
            return false;
        }
        
        if (effectiveDate != null && now.isBefore(effectiveDate)) {
            return false;
        }
        
        if (expiryDate != null && now.isAfter(expiryDate)) {
            return false;
        }
        
        return true;
    }

    public boolean isMandatory() {
        return ruleCategory == RuleCategory.MANDATORY;
    }

    public boolean isOptional() {
        return ruleCategory == RuleCategory.OPTIONAL;
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }

    public boolean isNotYetEffective() {
        return effectiveDate != null && LocalDate.now().isBefore(effectiveDate);
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void setEffectiveNow() {
        this.effectiveDate = LocalDate.now();
    }

    public void expireNow() {
        this.expiryDate = LocalDate.now();
    }
} 