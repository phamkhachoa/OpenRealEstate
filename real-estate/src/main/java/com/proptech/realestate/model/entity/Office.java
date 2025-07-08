package com.proptech.realestate.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a real estate office/brokerage within an MLS region.
 * Each office can have multiple agents and is associated with an MLS region.
 */
@Entity
@Table(name = "offices")
@Data
@EqualsAndHashCode(callSuper = false)
public class Office {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mls_region_id")
    private MLSRegion mlsRegion;

    // Office Information
    @Column(name = "office_name", nullable = false, length = 255)
    private String officeName;

    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "office_type", nullable = false, length = 50)
    private OfficeType officeType;

    // Contact Information
    @Column(length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(length = 500)
    private String website;

    // Address
    @Column(name = "street_address", length = 500)
    private String streetAddress;

    @Column(length = 100)
    private String city;

    @Column(length = 2)
    private String state;

    @Column(name = "zip_code", length = 20)
    private String zipCode;

    @Column(length = 2)
    private String country = "US";

    // Geographic coordinates
    private Double latitude;
    private Double longitude;

    // Business Details
    @Column(name = "established_date")
    private LocalDate establishedDate;

    @Column(name = "total_agents")
    private Integer totalAgents = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "franchise_name", length = 255)
    private String franchiseName;

    // Relationships
    @OneToMany(mappedBy = "office", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MLSMembership> memberships = new HashSet<>();

    @OneToMany(mappedBy = "listingOffice", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Listing> listings = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum OfficeType {
        BROKERAGE,      // Independent brokerage
        FRANCHISE,      // Franchise office (RE/MAX, Coldwell Banker, etc.)
        INDEPENDENT,    // Small independent office
        TEAM,           // Real estate team within a brokerage
        BRANCH_OFFICE   // Branch of a larger brokerage
    }

    // Business methods
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        
        if (streetAddress != null) {
            address.append(streetAddress);
        }
        
        if (city != null) {
            if (address.length() > 0) address.append(", ");
            address.append(city);
        }
        
        if (state != null) {
            if (address.length() > 0) address.append(", ");
            address.append(state);
        }
        
        if (zipCode != null) {
            if (address.length() > 0) address.append(" ");
            address.append(zipCode);
        }
        
        return address.toString();
    }

    public void addMembership(MLSMembership membership) {
        memberships.add(membership);
        membership.setOffice(this);
    }

    public void removeMembership(MLSMembership membership) {
        memberships.remove(membership);
        membership.setOffice(null);
    }

    public void addListing(Listing listing) {
        listings.add(listing);
        listing.setListingOffice(this);
    }

    public void removeListing(Listing listing) {
        listings.remove(listing);
        listing.setListingOffice(null);
    }

    public void incrementAgentCount() {
        this.totalAgents = (this.totalAgents == null ? 0 : this.totalAgents) + 1;
    }

    public void decrementAgentCount() {
        this.totalAgents = Math.max(0, (this.totalAgents == null ? 0 : this.totalAgents) - 1);
    }

    public boolean isInRegion(MLSRegion region) {
        return this.mlsRegion != null && this.mlsRegion.equals(region);
    }

    public boolean hasLocation() {
        return latitude != null && longitude != null;
    }
} 