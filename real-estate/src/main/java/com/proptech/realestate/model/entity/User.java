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

@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = false)
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(
            name = "UUID",
            strategy = "org.hibernate.id.UUIDGenerator"
    )
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    // MLS-specific fields
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", length = 20)
    private UserType userType = UserType.CONSUMER;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_state", length = 2)
    private String licenseState;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "broker_license_number", length = 50)
    private String brokerLicenseNumber;

    @Column(name = "specialties", length = 500)
    private String specialties; // Comma-separated specialties

    @Column(name = "years_experience")
    private Integer yearsExperience;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Professional Information
    @Column(name = "professional_title", length = 100)
    private String professionalTitle; // "Real Estate Agent", "Broker", etc.

    @Column(name = "company_name", length = 255)
    private String companyName;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "social_media_links", columnDefinition = "TEXT")
    private String socialMediaLinks; // JSON format

    // MLS Relationships
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<MLSMembership> mlsMemberships = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Listing> listings = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enums
    public enum UserType {
        CONSUMER,       // Regular website user
        AGENT,          // Licensed real estate agent
        BROKER,         // Licensed broker
        ADMIN,          // System administrator
        DEVELOPER,      // Property developer
        PROPERTY_MANAGER // Property management company
    }

    // Helper methods
    public String getFullName() {
        return fullName != null ? fullName : username;
    }

    public String getDisplayName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }
        return username;
    }

    public boolean isAgent() {
        return userType == UserType.AGENT;
    }

    public boolean isBroker() {
        return userType == UserType.BROKER;
    }

    public boolean isRealEstateProf() {
        return userType == UserType.AGENT || userType == UserType.BROKER;
    }

    public boolean isAdmin() {
        return userType == UserType.ADMIN;
    }

    public boolean hasValidLicense() {
        return licenseNumber != null && 
               licenseExpiry != null && 
               licenseExpiry.isAfter(LocalDate.now());
    }

    public boolean isLicenseExpiringSoon() {
        if (licenseExpiry == null) return false;
        return licenseExpiry.isBefore(LocalDate.now().plusDays(30));
    }

    public void addMLSMembership(MLSMembership membership) {
        mlsMemberships.add(membership);
        membership.setUser(this);
    }

    public void removeMLSMembership(MLSMembership membership) {
        mlsMemberships.remove(membership);
        membership.setUser(null);
    }

    public void addListing(Listing listing) {
        listings.add(listing);
        listing.setUser(this);
    }

    public void removeListing(Listing listing) {
        listings.remove(listing);
        listing.setUser(null);
    }

    // Get active MLS memberships
    public Set<MLSMembership> getActiveMemberships() {
        return mlsMemberships.stream()
            .filter(MLSMembership::isActive)
            .collect(java.util.stream.Collectors.toSet());
    }

    // Check if user has access to a specific MLS region
    public boolean hasAccessToRegion(MLSRegion region) {
        return mlsMemberships.stream()
            .anyMatch(membership -> 
                membership.isActive() && 
                membership.getMlsRegion().equals(region)
            );
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }
} 