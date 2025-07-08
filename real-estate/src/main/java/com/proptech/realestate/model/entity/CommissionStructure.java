package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents commission structures and policies within an MLS region.
 * Supports various commission types including percentage, flat fee, and tiered structures.
 */
@Entity
@Table(name = "commission_structures")
@Data
@EqualsAndHashCode(callSuper = false)
public class CommissionStructure {

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

    @Column(name = "structure_name", nullable = false, length = 255)
    private String structureName;

    @Enumerated(EnumType.STRING)
    @Column(name = "structure_type", nullable = false, length = 50)
    private StructureType structureType;

    // Commission rates (for percentage-based structures)
    @Column(name = "listing_side_percentage", precision = 5, scale = 2)
    private BigDecimal listingSidePercentage; // e.g., 3.00 for 3%

    @Column(name = "selling_side_percentage", precision = 5, scale = 2)
    private BigDecimal sellingSidePercentage; // e.g., 3.00 for 3%

    // Flat fee amounts
    @Column(name = "flat_fee_amount", precision = 10, scale = 2)
    private BigDecimal flatFeeAmount;

    @Column(name = "minimum_commission", precision = 10, scale = 2)
    private BigDecimal minimumCommission;

    @Column(name = "maximum_commission", precision = 10, scale = 2)
    private BigDecimal maximumCommission;

    // Tiered structure (stored as JSON)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tier_structure", columnDefinition = "jsonb")
    private String tierStructure;

    // Policies
    @Column(name = "allows_variable_commission", nullable = false)
    private Boolean allowsVariableCommission = true;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum StructureType {
        PERCENTAGE,     // Standard percentage-based commission
        FLAT_FEE,       // Fixed flat fee
        TIERED,         // Tiered based on price ranges
        CUSTOM,         // Custom structure requiring manual calculation
        GRADUATED,      // Graduated rates based on sale price
        BONUS_BASED     // Base rate plus performance bonuses
    }

    // Business methods
    public BigDecimal getTotalCommissionPercentage() {
        if (structureType == StructureType.PERCENTAGE) {
            BigDecimal listing = listingSidePercentage != null ? listingSidePercentage : BigDecimal.ZERO;
            BigDecimal selling = sellingSidePercentage != null ? sellingSidePercentage : BigDecimal.ZERO;
            return listing.add(selling);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal calculateCommission(BigDecimal salePrice) {
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal commission = BigDecimal.ZERO;

        switch (structureType) {
            case PERCENTAGE:
                BigDecimal totalPercentage = getTotalCommissionPercentage();
                commission = salePrice.multiply(totalPercentage.divide(BigDecimal.valueOf(100)));
                break;
                
            case FLAT_FEE:
                commission = flatFeeAmount != null ? flatFeeAmount : BigDecimal.ZERO;
                break;
                
            case TIERED:
                commission = calculateTieredCommission(salePrice);
                break;
                
            case CUSTOM:
                // Custom calculations would be handled by business logic
                commission = BigDecimal.ZERO;
                break;
                
            default:
                commission = BigDecimal.ZERO;
        }

        // Apply minimum and maximum limits
        if (minimumCommission != null && commission.compareTo(minimumCommission) < 0) {
            commission = minimumCommission;
        }
        
        if (maximumCommission != null && commission.compareTo(maximumCommission) > 0) {
            commission = maximumCommission;
        }

        return commission;
    }

    public BigDecimal calculateListingSideCommission(BigDecimal salePrice) {
        if (structureType == StructureType.PERCENTAGE && listingSidePercentage != null) {
            return salePrice.multiply(listingSidePercentage.divide(BigDecimal.valueOf(100)));
        }
        
        // For other types, split total commission in half
        return calculateCommission(salePrice).divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);
    }

    public BigDecimal calculateSellingSideCommission(BigDecimal salePrice) {
        if (structureType == StructureType.PERCENTAGE && sellingSidePercentage != null) {
            return salePrice.multiply(sellingSidePercentage.divide(BigDecimal.valueOf(100)));
        }
        
        // For other types, split total commission in half
        return calculateCommission(salePrice).divide(BigDecimal.valueOf(2), 2, BigDecimal.ROUND_HALF_UP);
    }

    private BigDecimal calculateTieredCommission(BigDecimal salePrice) {
        // This would parse the tierStructure JSON to calculate commission
        // For now, return a basic calculation
        if (listingSidePercentage != null && sellingSidePercentage != null) {
            return salePrice.multiply(getTotalCommissionPercentage().divide(BigDecimal.valueOf(100)));
        }
        return BigDecimal.ZERO;
    }

    public boolean isPercentageBased() {
        return structureType == StructureType.PERCENTAGE;
    }

    public boolean isFlatFee() {
        return structureType == StructureType.FLAT_FEE;
    }

    public boolean requiresManualCalculation() {
        return structureType == StructureType.CUSTOM;
    }

    public void makeDefault() {
        this.isDefault = true;
    }

    public void removeDefault() {
        this.isDefault = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
} 