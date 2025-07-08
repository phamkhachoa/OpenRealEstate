package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.proptech.realestate.model.entity.MLSRule;
import com.proptech.realestate.model.entity.CommissionStructure;

/**
 * Represents a Multiple Listing Service (MLS) region.
 * Each region can have its own rules, commission structures, and geographic boundaries.
 * Supports multi-tenancy for different MLS territories.
 */
@Entity
@Table(name = "mls_regions")
@Data
@EqualsAndHashCode(callSuper = false)
public class MLSRegion {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(name = "region_code", unique = true, nullable = false, length = 50)
    private String regionCode; // "seattle_mls", "bay_area_mls", "chicago_mls"

    @Column(nullable = false, length = 255)
    private String name; // "Seattle Multiple Listing Service"

    @Lob
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Geographic boundaries for the MLS region
    @Column(name = "north_boundary")
    private Double northBoundary;

    @Column(name = "south_boundary") 
    private Double southBoundary;

    @Column(name = "east_boundary")
    private Double eastBoundary;

    @Column(name = "west_boundary")
    private Double westBoundary;

    // MLS Configuration
    @Column(name = "listing_prefix", length = 10)
    private String listingPrefix; // "SEA", "SF", "CHI" for MLS numbers

    @Column(name = "max_listing_days")
    private Integer maxListingDays = 180; // Default listing duration

    @Column(name = "requires_approval")
    private Boolean requiresApproval = false;

    @Column(name = "allows_private_remarks")
    private Boolean allowsPrivateRemarks = true;

    @Column(name = "mandatory_showing_time_hours")
    private Integer mandatoryShowingTimeHours = 24;

    // Contact Information
    @Column(name = "admin_email", length = 255)
    private String adminEmail;

    @Column(name = "admin_phone", length = 20)
    private String adminPhone;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    // Business Rules and Commission Structures
    @OneToMany(mappedBy = "mlsRegion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MLSRule> rules = new HashSet<>();

    @OneToMany(mappedBy = "mlsRegion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<CommissionStructure> commissionStructures = new HashSet<>();

    // Memberships in this region
    @OneToMany(mappedBy = "mlsRegion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MLSMembership> memberships = new HashSet<>();

    // Listings in this region
    @OneToMany(mappedBy = "mlsRegion", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Listing> listings = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public String generateMLSNumber(Long sequence) {
        return String.format("%s%d%06d", 
            this.listingPrefix != null ? this.listingPrefix : "MLS",
            java.time.Year.now().getValue(),
            sequence);
    }

    public boolean isLocationWithinBoundaries(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return false;
        }

        return (northBoundary == null || latitude <= northBoundary) &&
               (southBoundary == null || latitude >= southBoundary) &&
               (eastBoundary == null || longitude <= eastBoundary) &&
               (westBoundary == null || longitude >= westBoundary);
    }

    public void addRule(MLSRule rule) {
        rules.add(rule);
        rule.setMlsRegion(this);
    }

    public void removeRule(MLSRule rule) {
        rules.remove(rule);
        rule.setMlsRegion(null);
    }

    public void addCommissionStructure(CommissionStructure commission) {
        commissionStructures.add(commission);
        commission.setMlsRegion(this);
    }

    public void removeCommissionStructure(CommissionStructure commission) {
        commissionStructures.remove(commission);
        commission.setMlsRegion(null);
    }
} 