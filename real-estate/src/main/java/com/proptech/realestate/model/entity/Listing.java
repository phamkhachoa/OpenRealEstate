package com.proptech.realestate.model.entity;

import com.proptech.realestate.model.enums.StandardStatus;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    // --- RESO Core Fields ---
    @Column(name = "listing_id", unique = true)
    private String listingId; // RESO: ListingId - The public, human-readable identifier.

    @Enumerated(EnumType.STRING)
    @Column(name = "standard_status", nullable = false)
    private StandardStatus standardStatus; // RESO: StandardStatus - e.g., Active, Pending, Closed

    @Column(name = "mls_status")
    private String mlsStatus; // RESO: MlsStatus - The local MLS status, mapped to StandardStatus.

    @Column(name = "list_price", precision = 15, scale = 2)
    private BigDecimal listPrice; // RESO: ListPrice

    @Column(name = "property_type")
    private String propertyType; // RESO: PropertyType - e.g., Residential, Commercial

    @Column(name = "property_sub_type")
    private String propertySubType; // RESO: PropertySubType - e.g., SingleFamilyResidence, Condominium

    @Column(name = "bedrooms_total")
    private Integer bedroomsTotal; // RESO: BedroomsTotal

    @Column(name = "bathrooms_total_integer")
    private Integer bathroomsTotalInteger; // RESO: BathroomsTotalInteger

    @Column(name = "living_area")
    private Double livingArea; // RESO: LivingArea

    @Column(name = "living_area_units")
    private String livingAreaUnits; // RESO: LivingAreaUnits - e.g., "Square Feet", "Square Meters"

    @Column(name = "lot_size_acres")
    private Double lotSizeAcres; // RESO: LotSizeAcres

    @Column(name = "lot_size_square_feet")
    private Double lotSizeSquareFeet; // RESO: LotSizeSquareFeet

    @Column(name = "year_built")
    private Integer yearBuilt; // RESO: YearBuilt

    // --- Address Fields (RESO compliant) ---
    @Column(name = "unparsed_address", length = 500)
    private String unparsedAddress; // RESO: UnparsedAddress

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "street_name")
    private String streetName;

    @Column(name = "city")
    private String city; // RESO: City

    @Column(name = "state_or_province")
    private String stateOrProvince; // RESO: StateOrProvince

    @Column(name = "postal_code")
    private String postalCode; // RESO: PostalCode

    @Column(name = "country")
    private String country; // RESO: Country

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude; // RESO: Latitude

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude; // RESO: Longitude

    // --- Other Important RESO Fields ---
    @Column(name = "listing_contract_date")
    private LocalDate listingContractDate; // RESO: ListingContractDate

    @Column(name = "on_market_date")
    private LocalDate onMarketDate; // RESO: OnMarketDate
    
    @Column(name = "close_date")
    private LocalDate closeDate; // RESO: CloseDate

    @Column(name = "close_price", precision = 15, scale = 2)
    private BigDecimal closePrice; // RESO: ClosePrice

    @Column(name = "public_remarks", columnDefinition = "TEXT")
    private String publicRemarks; // RESO: PublicRemarks

    @Column(name = "private_remarks", columnDefinition = "TEXT")
    private String privateRemarks; // RESO: PrivateRemarks

    @Column(name = "architectural_style")
    private String architecturalStyle; // RESO: ArchitecturalStyle

    @Column(name = "new_construction_yn")
    private Boolean newConstructionYn; // RESO: NewConstructionYN

    @Column(name = "photos_count")
    private Integer photosCount; // RESO: PhotosCount

    @Column(name = "videos_count")
    private Integer videosCount; // RESO: VideosCount

    // --- Timestamps (RESO compliant) ---
    @Column(name = "modification_timestamp", nullable = false)
    private LocalDateTime modificationTimestamp; // RESO: ModificationTimestamp

    @Column(name = "status_change_timestamp")
    private LocalDateTime statusChangeTimestamp; // RESO: StatusChangeTimestamp

    @Column(name = "photos_change_timestamp")
    private LocalDateTime photosChangeTimestamp; // RESO: PhotosChangeTimestamp

    // --- System & Relationship Fields (Not strictly RESO, but necessary) ---
    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Type(JsonType.class)
    @Column(name = "images", columnDefinition = "json")
    private List<String> images;

    @Type(JsonType.class)
    @Column(name = "amenities", columnDefinition = "json")
    private Map<String, Object> amenities;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_agent_id")
    private MLSMembership listingAgent; // Maps to RESO's ListAgent... fields

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_office_id")
    private Office listingOffice; // Maps to RESO's ListOffice... fields
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ListingAgreement listingAgreement;
    
    @PrePersist
    protected void onCreate() {
        modificationTimestamp = LocalDateTime.now();
        if (standardStatus == null) {
            standardStatus = StandardStatus.ComingSoon; // Default status
        }
    }

    @PreUpdate
    protected void onUpdate() {
        modificationTimestamp = LocalDateTime.now();
    }
}